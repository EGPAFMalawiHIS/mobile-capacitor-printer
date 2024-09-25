type PrinterOptions = { content: string; isHtml: boolean };

export type PrinterResponse = { ok: boolean };
export interface HtmlToPdfSaverPlugin {
  printWebPage(options: PrinterOptions): Promise<PrinterResponse>;

  printWebPageUsingSilentPrinter(
    options: PrinterOptions,
  ): Promise<PrinterResponse>;
}
