# html-to-pdf-saver

HtmlToPdfSaver Capacitor Plugin

## Install

```bash
npm install html-to-pdf-saver
npx cap sync
```

## API

<docgen-index>

* [`printWebPage(...)`](#printwebpage)
* [`printWebPageUsingSilentPrinter(...)`](#printwebpageusingsilentprinter)
* [`printWebPageToNetworkPrinter(...)`](#printwebpagetonetworkprinter)
* [`checkNetworkStatus()`](#checknetworkstatus)
* [Type Aliases](#type-aliases)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### printWebPage(...)

```typescript
printWebPage(options: PrinterOptions) => Promise<PrinterResponse>
```

| Param         | Type                                                      |
| ------------- | --------------------------------------------------------- |
| **`options`** | <code><a href="#printeroptions">PrinterOptions</a></code> |

**Returns:** <code>Promise&lt;<a href="#printerresponse">PrinterResponse</a>&gt;</code>

--------------------


### printWebPageUsingSilentPrinter(...)

```typescript
printWebPageUsingSilentPrinter(options: PrinterOptions) => Promise<PrinterResponse>
```

| Param         | Type                                                      |
| ------------- | --------------------------------------------------------- |
| **`options`** | <code><a href="#printeroptions">PrinterOptions</a></code> |

**Returns:** <code>Promise&lt;<a href="#printerresponse">PrinterResponse</a>&gt;</code>

--------------------


### printWebPageToNetworkPrinter(...)

```typescript
printWebPageToNetworkPrinter(options: { content: string; printerIp: string; printerPort: number; }) => Promise<PrinterResponse>
```

| Param         | Type                                                                      |
| ------------- | ------------------------------------------------------------------------- |
| **`options`** | <code>{ content: string; printerIp: string; printerPort: number; }</code> |

**Returns:** <code>Promise&lt;<a href="#printerresponse">PrinterResponse</a>&gt;</code>

--------------------


### checkNetworkStatus()

```typescript
checkNetworkStatus() => Promise<{ wifiConnected: boolean; vpnActive: boolean; status: boolean; }>
```

**Returns:** <code>Promise&lt;{ wifiConnected: boolean; vpnActive: boolean; status: boolean; }&gt;</code>

--------------------


### Type Aliases


#### PrinterResponse

<code>{ ok: boolean }</code>


#### PrinterOptions

<code>{ content: string; isHtml: boolean }</code>

</docgen-api>
