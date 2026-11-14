const {test} = require('node:test');
const assert = require('node:assert/strict');
const {version, replaceVersion, pinDependencies} = require('./release.cjs');
test('strict release version parsing', () => {
  assert.equal(version('projectVersion=1.2.3-SNAPSHOT\r\n').number, '1.2.3');
  for (const value of ['01.2.3', '1.2', '1.2.3-rc1', '1.2.3\nprojectVersion=2.0.0']) assert.throws(() => version(`projectVersion=${value}`));
});
test('prepare and candidate preserve immutable refs and allow upload retries', () => {
  const fs = require('node:fs');
  const os = require('node:os');
  const path = require('node:path');
  const {execFileSync} = require('node:child_process');
  const directory = fs.mkdtempSync(path.join(os.tmpdir(), 'voxelbrew-release-test-'));
  const remote = path.join(directory, 'remote.git');
  const work = path.join(directory, 'work');
  const env = {...process.env, GIT_AUTHOR_NAME:'Test', GIT_AUTHOR_EMAIL:'test@example.invalid', GIT_COMMITTER_NAME:'Test', GIT_COMMITTER_EMAIL:'test@example.invalid'};
  const run = (bin, args, cwd = work, extra = {}) => execFileSync(bin, args, {cwd, env:{...env,...extra}, encoding:'utf8', stdio:'pipe'}).trim();
  const git = (...args) => run('git', args);
  const script = path.join(__dirname, 'release.cjs');
  try {
    run('git', ['init','--bare',remote], directory);
    run('git', ['init','-b','master',work], directory);
    fs.writeFileSync(path.join(work, 'gradle.properties'), 'projectVersion=0.1.0-SNAPSHOT\nbomVersion=0.1.0-SNAPSHOT\n');
    git('add','.'); git('commit','-m','Initial'); git('remote','add','origin',remote); git('push','origin','master');
    const base = git('rev-parse','HEAD');
    const common = {GITHUB_REPOSITORY:'Voxel-Brew/api-export'};
    run(process.execPath, [script,'prepare'], work, {...common,GITHUB_REF_NAME:'master',GITHUB_SHA:base});
    assert.match(git('show','HEAD:gradle.properties'), /projectVersion=0.2.0-SNAPSHOT/);
    git('fetch','origin','release/0.1.x');
    const source = git('rev-parse','FETCH_HEAD');
    assert.match(git('show',`${source}:gradle.properties`), /bomVersion=0.1.0$/);
    const releaseEnv = {...common,GITHUB_REF_NAME:'release/0.1.x',GITHUB_SHA:source};
    run(process.execPath,[script,'candidate'],work,releaseEnv);
    assert.equal(git('ls-remote','--tags','origin'), '');
    assert.match(fs.readFileSync(path.join(work,'gradle.properties'),'utf8'), /projectVersion=0.1.0\r?\n/);
    run(process.execPath,[script,'tag'],work,releaseEnv);
    const tagged = git('rev-parse','v0.1.0^{commit}');
    run(process.execPath,[script,'candidate'],work,releaseEnv);
    assert.equal(git('rev-parse','HEAD'), tagged);
    assert.equal(git('ls-remote','--heads','origin','release/0.1.x').split(/\s/)[0],source);
    assert.throws(() => run(process.execPath,[script,'prepare'],work,{...common,GITHUB_REF_NAME:'master',GITHUB_SHA:base}));
  } finally {
    assert(path.resolve(directory).startsWith(path.resolve(os.tmpdir()) + path.sep));
    fs.rmSync(directory,{recursive:true,force:true});
  }
});
test('version replacement leaves dependencies unchanged', () => {
  assert.equal(replaceVersion('projectVersion=1.2.3-SNAPSHOT\nbomVersion=1.0.0\n', '1.2.3'), 'projectVersion=1.2.3\nbomVersion=1.0.0\n');
});
test('pin the BOM for every API and reject dynamic versions', () => {
  const text = 'bomVersion=1.2.3-SNAPSHOT\nexportVersion=2.3.4-SNAPSHOT\n';
  assert.equal(pinDependencies(text, 'model', {}), 'bomVersion=1.2.3\nexportVersion=2.3.4-SNAPSHOT\n');
  assert.equal(pinDependencies(text, 'slicer', {}), 'bomVersion=1.2.3\nexportVersion=2.3.4-SNAPSHOT\n');
  assert.match(pinDependencies(text, 'export', {BOM_VERSION:'2.0.0'}), /^bomVersion=2.0.0/);
  for (const value of ['1.+', '1.0.0-SNAPSHOT', 'latest.release']) assert.throws(() => pinDependencies(text, 'export', {BOM_VERSION:value}));
});
