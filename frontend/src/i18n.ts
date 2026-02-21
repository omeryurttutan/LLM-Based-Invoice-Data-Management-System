import { getRequestConfig } from 'next-intl/server';
import { cookies } from 'next/headers';
import trMessages from './messages/tr';
import enMessages from './messages/en';

export default getRequestConfig(async () => {
    const cookieStore = cookies();
    const locale = cookieStore.get('NEXT_LOCALE')?.value || 'tr';

    return {
        locale,
        messages: locale === 'en' ? enMessages : trMessages,
        timeZone: 'Europe/Istanbul'
    };
});
