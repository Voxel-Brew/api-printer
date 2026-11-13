const fs = require('node:fs');
const assert = require('node:assert/strict');
const lines = fs.readFileSync(process.argv[2], 'utf8').split('\n').map(line => line.split(':'));
const id = process.env.SIGNING_KEY_ID?.toUpperCase().replace(/^0X/, '');
assert(id && /^[A-F0-9]{8,40}$/.test(id), 'Invalid signing key ID');
const now = Math.floor(Date.now() / 1000);
const primary = lines.find(row => ['sec', 'pub'].includes(row[0]));
assert(primary && !['r','e','d'].includes(primary[1]) && (!primary[6] || Number(primary[6]) > now), 'Primary key is revoked, disabled or expired');
let selected;
for (let i = 0; i < lines.length; i++) {
  const row = lines[i];
  if (!['sec','pub','ssb','sub'].includes(row[0])) continue;
  const fingerprint = lines[i + 1]?.[0] === 'fpr' ? lines[i + 1][9] : '';
  if (row[4].endsWith(id) || fingerprint.endsWith(id)) selected = row;
}
assert(selected && selected[11].includes('s'), 'Configured key is missing or cannot sign');
assert(!['r','e','d'].includes(selected[1]), 'Signing key is revoked, disabled or expired');
assert(!selected[6] || Number(selected[6]) > now, 'Signing key has expired');
console.log('Signing key metadata is valid');
