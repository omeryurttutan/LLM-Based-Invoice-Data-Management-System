import tr from '@/messages/tr';

type Messages = typeof tr;

declare global {
    // Use type safe message keys with `next-intl`
    interface IntlMessages extends Messages { }
}
