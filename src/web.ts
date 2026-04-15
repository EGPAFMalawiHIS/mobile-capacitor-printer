import { WebPlugin } from '@capacitor/core';

import type { HtmlToPdfSaverPlugin, PrinterResponse } from './definitions';

export class HtmlToPdfSaverWeb
  extends WebPlugin
  implements HtmlToPdfSaverPlugin {
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

  async printWebPageToNetworkPrinter(options: {
    content: string;
    printerIp: string;
    printerPort: number
  }): Promise<PrinterResponse> {

    console.warn(
      'printWebPageUsingSilentPrinter is not supported on the web platform.',
      options.content,
      options.printerIp,
      options.printerPort,
    );

    return { ok: false }
  }

  async checkNetworkStatus(): Promise<{ wifiConnected: boolean; vpnActive: boolean; status: boolean }> {
    return {
      wifiConnected: false,
      vpnActive: false,
      status: false
    }
  }
}
