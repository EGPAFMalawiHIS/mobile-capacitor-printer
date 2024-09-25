import { WebPlugin } from '@capacitor/core';

import type { HtmlToPdfSaverPlugin, PrinterResponse } from './definitions';

export class HtmlToPdfSaverWeb
  extends WebPlugin
  implements HtmlToPdfSaverPlugin
{
  async printWebPage(options: {
    content: string;
    isHtml: boolean;
  }): Promise<PrinterResponse> {
    console.warn(
      'printWebPage is not supported on the web platform.',
      options.content,
      options.isHtml,
    );

    return { ok: false };
  }

  async printWebPageUsingSilentPrinter(options: {
    content: string;
    isHtml: boolean;
  }): Promise<PrinterResponse> {
    console.warn(
      'printWebPageUsingSilentPrinter is not supported on the web platform.',
      options.content,
      options.isHtml,
    );
    return { ok: false };
  }
}
