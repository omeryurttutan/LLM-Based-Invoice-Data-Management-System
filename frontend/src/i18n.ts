import { getRequestConfig } from 'next-intl/server';
import { cookies } from 'next/headers';

export default getRequestConfig(async () => {
    const cookieStore = cookies();
    const locale = cookieStore.get('NEXT_LOCALE')?.value || 'tr';

    return {
        locale,
        messages: (await import(`./messages/${locale}`)).default,
        timeZone: 'Europe/Istanbul'
    };
});
