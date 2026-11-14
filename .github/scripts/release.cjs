const fs = require('node:fs');
const {execFileSync} = require('node:child_process');
const assert = require('node:assert/strict');
function version(text) {
  const matches = [...text.matchAll(/^projectVersion=(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(-SNAPSHOT)?\r?$/gm)];
  assert.equal(matches.length, 1, 'Expected exactly one projectVersion=A.B.C[-SNAPSHOT]');
  const [, major, minor, patch, snapshot] = matches[0];
  return {major: Number(major), minor: Number(minor), patch: Number(patch), snapshot: !!snapshot, number: `${major}.${minor}.${patch}`};
}
function replaceVersion(text, next) {
  version(text); version(`projectVersion=${next}`);
  return text.replace(/^projectVersion=.*$/m, `projectVersion=${next}`);
}
function pinDependencies(text, name, env = process.env) {
  const used = ['bom'];
  for (const dependency of used) {
    const pattern = new RegExp(`^${dependency}Version=(.+)\\r?$`, 'm');
    const match = text.match(pattern);
    assert(match, `Missing ${dependency}Version`);
    const target = env[`${dependency.toUpperCase()}_VERSION`] || match[1].trim().replace(/-SNAPSHOT$/, '');
    assert(/^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)$/.test(target), 'Dependencies must use exact releases');
    text = text.replace(pattern, `${dependency}Version=${target}`);
  }
  return text;
}
function main(command) {
  const run = (bin, args, opts = {}) => execFileSync(bin, args, {encoding:'utf8', ...opts}).trim();
  const git = (...args) => run('git', args);
  const read = () => fs.readFileSync('gradle.properties', 'utf8');
  const remote = ref => git('ls-remote', '--refs', 'origin', ref).split(/\s/)[0];
  const commit = (text, message) => {
    fs.writeFileSync('gradle.properties', text);
    git('add', 'gradle.properties'); git('commit', '-m', message);
    return git('rev-parse', 'HEAD');
  };
  const planFile = '.git/api-release.json';
  const branch = process.env.GITHUB_REF_NAME;
  const selected = process.env.GITHUB_SHA;
  git('config', 'user.name', 'github-actions[bot]');
  git('config', 'user.email', '41898282+github-actions[bot]@users.noreply.github.com');
  assert.equal(git('status', '--porcelain', '--untracked-files=no'), '', 'Tracked working tree must be clean');
  if (command === 'prepare' || command === 'candidate') {
    assert(/^[a-f0-9]{40}$/.test(selected), 'Full source SHA required');
    assert.equal(remote(`refs/heads/${branch}`), selected, 'Branch advanced; dispatch again');
    git('checkout', '--detach', selected);
    const original = read();
    const v = version(original);
    assert(v.snapshot, 'Source must be a SNAPSHOT');
    const releaseBranch = `release/${v.major}.${v.minor}.x`;
    if (command === 'prepare') {
      assert.equal(branch, 'master', 'Prepare on master');
      assert.equal(remote(`refs/heads/${releaseBranch}`), '', 'Release branch already exists');
      const name = process.env.GITHUB_REPOSITORY.split('/')[1].replace(/^api-/, '');
      const pinned = pinDependencies(original, name);
      const release = pinned === original ? selected : commit(pinned, 'Pin API release dependencies');
      git('checkout', '--detach', selected);
      const next = `${v.major}.${v.minor + 1}.0-SNAPSHOT`;
      const head = commit(replaceVersion(original, next), `Begin ${next} development`);
      git('push', '--atomic', `--force-with-lease=refs/heads/master:${selected}`, `--force-with-lease=refs/heads/${releaseBranch}:`, 'origin', `${release}:refs/heads/${releaseBranch}`, `${head}:refs/heads/master`);
      console.log(`Prepared ${releaseBranch}; master is now ${next}`);
      return;
    }
    assert.equal(branch, releaseBranch, 'Publish from release/A.B.x');
    const tag = `v${v.number}`;
    const candidate = commit(replaceVersion(original, v.number), `Release ${v.number}`);
    if (remote(`refs/tags/${tag}`)) {
      git('fetch', 'origin', `refs/tags/${tag}:refs/tags/${tag}`);
      assert.equal(git('rev-parse', `${tag}^{tree}`), git('rev-parse', `${candidate}^{tree}`), 'Existing tag has different content');
      assert.equal(git('rev-parse', `${tag}^`), selected, 'Existing tag has a different source');
      git('checkout', '--detach', `${tag}^{commit}`);
    }
    fs.writeFileSync(planFile, JSON.stringify({branch, base:selected, release:git('rev-parse','HEAD'), tag, version:v.number, next:`${v.major}.${v.minor}.${v.patch + 1}-SNAPSHOT`}));
    console.log(`Candidate ${tag} ready; no remote refs changed`);
    return;
  }
  const p = JSON.parse(fs.readFileSync(planFile));
  assert.equal(git('rev-parse', 'HEAD'), p.release, 'Wrong candidate checkout');
  assert.equal(remote(`refs/heads/${p.branch}`), p.base, 'Release branch advanced');
  if (command === 'tag') {
    if (!remote(`refs/tags/${p.tag}`)) {
      git('tag', '-a', p.tag, '-m', `Release ${p.version}`);
      git('push', `--force-with-lease=refs/tags/${p.tag}:`, 'origin', `refs/tags/${p.tag}`);
    }
  } else if (command === 'finish') {
    assert.equal(git('rev-parse', `${p.tag}^{commit}`), p.release, 'Release tag mismatch');
    // Do not move the branch until Central has reported PUBLISHED and GitHub release exists.
    let exists = false;
    try { run('gh', ['release','view',p.tag]); exists = true; } catch {}
    if (!exists) run('gh', ['release','create',p.tag,'--verify-tag','--title',p.tag,'--notes',`Maven Central release: dev.voxelbrew.api:${process.env.GITHUB_REPOSITORY.split('/')[1].replace(/^api-/, '')}:${p.version}`]);
    const next = commit(replaceVersion(read(), p.next), `Begin ${p.next} development`);
    git('push', `--force-with-lease=refs/heads/${p.branch}:${p.base}`, 'origin', `${next}:refs/heads/${p.branch}`);
  } else throw new Error('Unknown release command');
}
module.exports = {version, replaceVersion, pinDependencies};
if (require.main === module) main(process.argv[2]);
