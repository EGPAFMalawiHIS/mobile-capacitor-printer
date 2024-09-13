import { registerPlugin } from '@capacitor/core';

import type { HtmlToPdfSaverPlugin } from './definitions';

const HtmlToPdfSaver = registerPlugin<HtmlToPdfSaverPlugin>('HtmlToPdfSaver', {
  web: () => import('./web').then(m => new m.HtmlToPdfSaverWeb()),
});

export * from './definitions';
export { HtmlToPdfSaver };
