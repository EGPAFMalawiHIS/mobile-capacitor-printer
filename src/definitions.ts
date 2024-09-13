export interface HtmlToPdfSaverPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
}
