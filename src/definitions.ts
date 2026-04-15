type PrinterOptions = { content: string; isHtml: boolean };

export type PrinterResponse = { ok: boolean };
export interface HtmlToPdfSaverPlugin {
  printWebPage(options: PrinterOptions): Promise<PrinterResponse>;

  printWebPageUsingSilentPrinter(
    options: PrinterOptions,
  ): Promise<PrinterResponse>;

  printWebPageToNetworkPrinter(options: {
    content: string;
    printerIp: string;
    printerPort: number
  }): Promise<PrinterResponse>

  checkNetworkStatus(): Promise<{ wifiConnected: boolean; vpnActive: boolean; status: boolean }>
}
