const { format } = require('date-fns');
const { tr } = require('date-fns/locale');

const date = new Date(2026, 1, 23); // A Monday
console.log('E:', format(date, 'E', { locale: tr }));
console.log('EE:', format(date, 'EE', { locale: tr }));
console.log('EEE:', format(date, 'EEE', { locale: tr }));
console.log('EEEE:', format(date, 'EEEE', { locale: tr }));
console.log('EEEEE:', format(date, 'EEEEE', { locale: tr }));
console.log('EEEEEE:', format(date, 'EEEEEE', { locale: tr }));
