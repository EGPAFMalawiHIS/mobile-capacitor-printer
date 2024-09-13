import { WebPlugin } from '@capacitor/core';

import type { HtmlToPdfSaverPlugin } from './definitions';

export class HtmlToPdfSaverWeb
  extends WebPlugin
  implements HtmlToPdfSaverPlugin
{
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
}
