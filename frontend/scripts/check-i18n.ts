import fs from 'fs';
import path from 'path';

const messagesDir = path.join(process.cwd(), 'src/messages');
const trDir = path.join(messagesDir, 'tr');
const enDir = path.join(messagesDir, 'en');

function getKeys(obj: any, prefix = ''): string[] {
    let keys: string[] = [];
    for (const key in obj) {
        if (typeof obj[key] === 'object' && obj[key] !== null) {
            keys = keys.concat(getKeys(obj[key], prefix + key + '.'));
        } else {
            keys.push(prefix + key);
        }
    }
    return keys;
}

function compareFiles(filename: string) {
    const trPath = path.join(trDir, filename);
    const enPath = path.join(enDir, filename);

    if (!fs.existsSync(trPath)) {
        console.error(`TR file missing: ${filename}`);
        return;
    }
    if (!fs.existsSync(enPath)) {
        console.error(`EN file missing: ${filename}`);
        return;
    }

    const trContent = JSON.parse(fs.readFileSync(trPath, 'utf-8'));
    const enContent = JSON.parse(fs.readFileSync(enPath, 'utf-8'));

    const trKeys = getKeys(trContent);
    const enKeys = getKeys(enContent);

    const missingInEn = trKeys.filter(k => !enKeys.includes(k));
    const missingInTr = enKeys.filter(k => !trKeys.includes(k));

    if (missingInEn.length > 0) {
        console.log(`\nMissing keys in EN (${filename}):`);
        missingInEn.forEach(k => console.log(`  - ${k}`));
    }

    if (missingInTr.length > 0) {
        console.log(`\nMissing keys in TR (${filename}):`);
        missingInTr.forEach(k => console.log(`  - ${k}`));
    }

    if (missingInEn.length === 0 && missingInTr.length === 0) {
        console.log(`OK: ${filename}`);
    }
}

console.log('Checking translation completeness...');
const files = fs.readdirSync(trDir).filter(f => f.endsWith('.json'));
files.forEach(compareFiles);
