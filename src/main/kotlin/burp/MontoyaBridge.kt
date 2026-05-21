package burp

import burp.api.montoya.MontoyaApi
import burp.api.montoya.core.Annotations
import burp.api.montoya.core.HighlightColor
import burp.api.montoya.http.HttpService
import burp.api.montoya.http.message.HttpRequestResponse
import burp.api.montoya.http.message.requests.HttpRequest
import burp.api.montoya.http.message.responses.HttpResponse
import burp.api.montoya.http.message.params.HttpParameter
import burp.api.montoya.http.message.params.HttpParameterType
import burp.api.montoya.http.handler.*
import burp.api.montoya.ui.contextmenu.ContextMenuEvent
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider
import burp.api.montoya.ui.editor.EditorOptions
import burp.api.montoya.ui.editor.HttpRequestEditor
import burp.api.montoya.ui.editor.HttpResponseEditor
import burp.api.montoya.core.ToolType
import burp.api.montoya.scanner.AuditConfiguration
import burp.api.montoya.scanner.BuiltInAuditConfiguration
import burpwrappers.SerializableHttpRequestResponse
import burpwrappers.SerializableHttpService
import java.io.OutputStream
import java.io.PrintWriter
import java.net.URL
import javax.swing.JComponent
import javax.swing.JMenuItem

class BurpExtenderCallbacksMontoya(val api: MontoyaApi) : IBurpExtenderCallbacks {
    private val helpersInstance = ExtensionHelpersMontoya(api)

    override fun setExtensionName(name: String) {
        api.extension().setName(name)
    }

    override fun printOutput(output: String) {
        PrintWriter(stdout, true).println(output)
    }

    override fun printError(error: String) {
        PrintWriter(stderr, true).println(error)
    }

    override val stdout: OutputStream
        get() = api.logging().output()

    override val stderr: OutputStream
        get() = api.logging().error()

    override val helpers: IExtensionHelpers
        get() = helpersInstance

    private val extensionStateListenersList = mutableListOf<IExtensionStateListener>()
    override val extensionStateListeners: List<IExtensionStateListener>
        get() = extensionStateListenersList

    private val httpListenersList = mutableListOf<IHttpListener>()
    override val httpListeners: List<IHttpListener>
        get() = httpListenersList

    override val proxyListeners: List<IProxyListener> get() = emptyList()
    override val scannerListeners: List<IScannerListener> get() = emptyList()
    override val scopeChangeListeners: List<IScopeChangeListener> get() = emptyList()
    override val contextMenuFactories: List<IContextMenuFactory> get() = emptyList()
    override val messageEditorTabFactories: List<IMessageEditorTabFactory> get() = emptyList()
    override val scannerInsertionPointProviders: List<IScannerInsertionPointProvider> get() = emptyList()
    override val scannerChecks: List<IScannerCheck> get() = emptyList()
    override val intruderPayloadGeneratorFactories: List<IIntruderPayloadGeneratorFactory> get() = emptyList()
    override val intruderPayloadProcessors: List<IIntruderPayloadProcessor> get() = emptyList()
    override val sessionHandlingActions: List<ISessionHandlingAction> get() = emptyList()
    override val commandLineArguments: Array<String> get() = emptyArray()
    override val proxyHistory: Array<IHttpRequestResponse> get() = emptyArray()
    override val cookieJarContents: List<ICookie> get() = emptyList()
    override val burpVersion: Array<String> get() = arrayOf("Burp Suite", "Montoya", "2026.2")
    override val extensionFilename: String get() = "PentagridScanController.jar"
    override val isExtensionBapp: Boolean get() = false

    override fun registerExtensionStateListener(listener: IExtensionStateListener) {
        extensionStateListenersList.add(listener)
        api.extension().registerUnloadingHandler {
            listener.extensionUnloaded()
        }
    }

    override fun registerHttpListener(listener: IHttpListener) {
        httpListenersList.add(listener)
        api.http().registerHttpHandler(HttpListenerMontoya(listener))
    }

    override fun registerContextMenuFactory(factory: IContextMenuFactory) {
        api.userInterface().registerContextMenuItemsProvider(ContextMenuFactoryMontoya(api, factory))
    }

    override fun makeHttpRequest(httpService: IHttpService, request: ByteArray): IHttpRequestResponse {
        val montoyaService = HttpService.httpService(httpService.host, httpService.port, httpService.protocol == "https")
        val montoyaRequest = HttpRequest.httpRequest(montoyaService, burp.api.montoya.core.ByteArray.byteArray(*request))
        val montoyaResponse = api.http().sendRequest(montoyaRequest)
        return SerializableHttpRequestResponse(
            request,
            montoyaResponse.response().toByteArray().getBytes(),
            null,
            null,
            httpService
        )
    }

    override fun makeHttpRequest(host: String, port: Int, useHttps: Boolean, request: ByteArray): ByteArray? {
        val montoyaService = HttpService.httpService(host, port, useHttps)
        val montoyaRequest = HttpRequest.httpRequest(montoyaService, burp.api.montoya.core.ByteArray.byteArray(*request))
        val montoyaResponse = api.http().sendRequest(montoyaRequest)
        return montoyaResponse.response().toByteArray().getBytes()
    }

    override fun saveExtensionSetting(name: String, value: String) {
        api.persistence().preferences().setString(name, value)
    }

    override fun loadExtensionSetting(name: String): String? {
        return api.persistence().preferences().getString(name)
    }

    override fun addToSiteMap(item: IHttpRequestResponse) {
        val montoyaService = HttpService.httpService(item.httpService.host, item.httpService.port, item.httpService.protocol == "https")
        val montoyaRequest = HttpRequest.httpRequest(montoyaService, burp.api.montoya.core.ByteArray.byteArray(*item.request))
        val montoyaResponse = item.response?.let { HttpResponse.httpResponse(burp.api.montoya.core.ByteArray.byteArray(*it)) }
        val rr = HttpRequestResponse.httpRequestResponse(montoyaRequest, montoyaResponse)
        api.siteMap().add(rr)
    }

    override fun getSiteMap(urlPrefix: String?): Array<IHttpRequestResponse> {
        val items = api.siteMap().requestResponses()
        val result = mutableListOf<IHttpRequestResponse>()
        for (item in items) {
            if (urlPrefix == null || item.request().url().startsWith(urlPrefix)) {
                val service = item.request().httpService()
                val legacyService = SerializableHttpService(service.host(), service.port().toInt(), if (service.secure()) "https" else "http")
                val legacyItem = SerializableHttpRequestResponse(
                    item.request().toByteArray().getBytes(),
                    item.response()?.toByteArray()?.getBytes(),
                    null,
                    null,
                    legacyService
                )
                result.add(legacyItem)
            }
        }
        return result.toTypedArray()
    }

    override fun getToolName(toolFlag: Int): String {
        return when (toolFlag) {
            IBurpExtenderCallbacks.TOOL_PROXY -> "Proxy"
            IBurpExtenderCallbacks.TOOL_REPEATER -> "Repeater"
            IBurpExtenderCallbacks.TOOL_SCANNER -> "Scanner"
            IBurpExtenderCallbacks.TOOL_SPIDER -> "Spider"
            IBurpExtenderCallbacks.TOOL_INTRUDER -> "Intruder"
            IBurpExtenderCallbacks.TOOL_SEQUENCER -> "Sequencer"
            IBurpExtenderCallbacks.TOOL_DECODER -> "Decoder"
            IBurpExtenderCallbacks.TOOL_COMPARER -> "Comparer"
            IBurpExtenderCallbacks.TOOL_TARGET -> "Target"
            0x00001337 -> "User"
            else -> "Unknown"
        }
    }

    override fun customizeUiComponent(component: java.awt.Component) {
        api.userInterface().applyThemeToComponent(component)
    }

    override fun addSuiteTab(tab: ITab) {
        api.userInterface().registerSuiteTab(tab.tabCaption, tab.uiComponent)
    }

    override fun createMessageEditor(controller: IMessageEditorController, editable: Boolean): IMessageEditor {
        val requestEditor = if (editable) {
            api.userInterface().createHttpRequestEditor()
        } else {
            api.userInterface().createHttpRequestEditor(EditorOptions.READ_ONLY)
        }
        val responseEditor = if (editable) {
            api.userInterface().createHttpResponseEditor()
        } else {
            api.userInterface().createHttpResponseEditor(EditorOptions.READ_ONLY)
        }
        return MessageEditorMontoya(requestEditor, responseEditor)
    }

    override fun isInScope(url: URL): Boolean {
        return api.scope().isInScope(url.toString())
    }

    override fun doActiveScan(host: String, port: Int, useHttps: Boolean, request: ByteArray): IScanQueueItem {
        val montoyaService = HttpService.httpService(host, port, useHttps)
        val montoyaRequest = HttpRequest.httpRequest(montoyaService, burp.api.montoya.core.ByteArray.byteArray(*request))
        val audit = api.scanner().startAudit(
            AuditConfiguration.auditConfiguration(
                BuiltInAuditConfiguration.LEGACY_ACTIVE_AUDIT_CHECKS
            )
        )
        audit.addRequest(montoyaRequest)
        return ScanQueueItemMontoya()
    }

    override fun doActiveScan(
        host: String,
        port: Int,
        useHttps: Boolean,
        request: ByteArray,
        insertionPointOffsets: List<IntArray>
    ): IScanQueueItem {
        val montoyaService = HttpService.httpService(host, port, useHttps)
        val montoyaRequest = HttpRequest.httpRequest(montoyaService, burp.api.montoya.core.ByteArray.byteArray(*request))
        val audit = api.scanner().startAudit(
            AuditConfiguration.auditConfiguration(
                BuiltInAuditConfiguration.LEGACY_ACTIVE_AUDIT_CHECKS
            )
        )
        audit.addRequest(montoyaRequest)
        return ScanQueueItemMontoya()
    }

    override fun saveBuffersToTempFiles(httpRequestResponse: IHttpRequestResponse): IHttpRequestResponsePersisted {
        return HttpRequestResponsePersistedDummy(httpRequestResponse)
    }

    override fun createBurpCollaboratorClientContext(): IBurpCollaboratorClientContext? {
        val client = api.collaborator().createClient()
        return BurpCollaboratorClientContextMontoya(client)
    }

    override fun removeExtensionStateListener(listener: IExtensionStateListener) {}
    override fun removeHttpListener(listener: IHttpListener) {}
    override fun registerProxyListener(listener: IProxyListener) {}
    override fun removeProxyListener(listener: IProxyListener) {}
    override fun registerScannerListener(listener: IScannerListener) {}
    override fun removeScannerListener(listener: IScannerListener) {}
    override fun registerScopeChangeListener(listener: IScopeChangeListener) {}
    override fun removeScopeChangeListener(listener: IScopeChangeListener) {}
    override fun removeContextMenuFactory(factory: IContextMenuFactory) {}
    override fun registerMessageEditorTabFactory(factory: IMessageEditorTabFactory) {}
    override fun removeMessageEditorTabFactory(factory: IMessageEditorTabFactory) {}
    override fun registerScannerInsertionPointProvider(provider: IScannerInsertionPointProvider) {}
    override fun removeScannerInsertionPointProvider(provider: IScannerInsertionPointProvider) {}
    override fun registerScannerCheck(check: IScannerCheck) {}
    override fun removeScannerCheck(check: IScannerCheck) {}
    override fun registerIntruderPayloadGeneratorFactory(factory: IIntruderPayloadGeneratorFactory) {}
    override fun removeIntruderPayloadGeneratorFactory(factory: IIntruderPayloadGeneratorFactory) {}
    override fun registerIntruderPayloadProcessor(processor: IIntruderPayloadProcessor) {}
    override fun removeIntruderPayloadProcessor(processor: IIntruderPayloadProcessor) {}
    override fun registerSessionHandlingAction(action: ISessionHandlingAction) {}
    override fun removeSessionHandlingAction(action: ISessionHandlingAction) {}
    override fun unloadExtension() {}
    override fun removeSuiteTab(tab: ITab) {}
    override fun createTextEditor(): ITextEditor = throw UnsupportedOperationException()
    override fun sendToRepeater(host: String, port: Int, useHttps: Boolean, request: ByteArray, tabCaption: String) {}
    override fun sendToIntruder(host: String, port: Int, useHttps: Boolean, request: ByteArray) {}
    override fun sendToIntruder(host: String, port: Int, useHttps: Boolean, request: ByteArray, payloadPositionOffsets: List<IntArray>) {}
    override fun sendToComparer(data: ByteArray) {}
    override fun sendToSpider(url: java.net.URL) {}
    override fun doPassiveScan(host: String, port: Int, useHttps: Boolean, request: ByteArray, response: ByteArray) {}
    override fun includeInScope(url: java.net.URL) {}
    override fun excludeFromScope(url: java.net.URL) {}
    override fun issueAlert(message: String) {}
    override fun getScanIssues(urlPrefix: String): Array<IScanIssue> = emptyArray()
    override fun generateScanReport(format: String, issues: Array<IScanIssue>, file: java.io.File) {}
    override fun updateCookieJar(cookie: ICookie) {}
    override fun restoreState(file: java.io.File) {}
    override fun saveState(file: java.io.File) {}
    override fun saveConfig(): Map<String, String> = emptyMap()
    override fun loadConfig(config: Map<String, String>) {}
    override fun saveConfigAsJson(vararg configPaths: String): String = ""
    override fun loadConfigFromJson(config: String) {}
    override fun setProxyInterceptionEnabled(enabled: Boolean) {}
    override fun exitSuite(promptUser: Boolean) {}
    override fun saveToTempFile(buffer: ByteArray): ITempFile = throw UnsupportedOperationException()
    override fun applyMarkers(httpRequestResponse: IHttpRequestResponse, requestMarkers: List<IntArray>, responseMarkers: List<IntArray>): IHttpRequestResponseWithMarkers = throw UnsupportedOperationException()
    override fun addScanIssue(issue: IScanIssue) {}
    override fun getParameters(request: ByteArray): Array<Array<String>> = emptyArray()
    override fun getHeaders(message: ByteArray): Array<String> = emptyArray()
    override fun registerMenuItem(menuItemCaption: String, menuItemHandler: IMenuItemHandler) {}
}

class ExtensionHelpersMontoya(val api: MontoyaApi) : IExtensionHelpers {
    override fun analyzeRequest(request: ByteArray): IRequestInfo {
        val montoyaReq = HttpRequest.httpRequest(burp.api.montoya.core.ByteArray.byteArray(*request))
        return RequestInfoMontoya(montoyaReq)
    }

    override fun analyzeRequest(httpService: IHttpService, request: ByteArray): IRequestInfo {
        val montoyaService = HttpService.httpService(httpService.host, httpService.port, httpService.protocol == "https")
        val montoyaReq = HttpRequest.httpRequest(montoyaService, burp.api.montoya.core.ByteArray.byteArray(*request))
        return RequestInfoMontoya(montoyaReq)
    }

    override fun analyzeRequest(request: IHttpRequestResponse): IRequestInfo {
        val montoyaReq = HttpRequest.httpRequest(burp.api.montoya.core.ByteArray.byteArray(*request.request))
        return RequestInfoMontoya(montoyaReq)
    }

    override fun analyzeResponse(response: ByteArray): IResponseInfo {
        val montoyaRes = HttpResponse.httpResponse(burp.api.montoya.core.ByteArray.byteArray(*response))
        return ResponseInfoMontoya(montoyaRes)
    }

    override fun bytesToString(bytes: ByteArray): String {
        return String(bytes, Charsets.ISO_8859_1)
    }

    override fun stringToBytes(s: String): ByteArray {
        return s.toByteArray(Charsets.ISO_8859_1)
    }

    override fun buildParameter(name: String, value: String, type: Byte): IParameter {
        return ParameterMontoya(name, value, type)
    }

    override fun updateParameter(request: ByteArray, parameter: IParameter): ByteArray {
        val montoyaReq = HttpRequest.httpRequest(burp.api.montoya.core.ByteArray.byteArray(*request))
        val paramType = when (parameter.type) {
            IParameter.PARAM_URL -> HttpParameterType.URL
            IParameter.PARAM_BODY -> HttpParameterType.BODY
            IParameter.PARAM_COOKIE -> HttpParameterType.COOKIE
            IParameter.PARAM_XML -> HttpParameterType.XML
            IParameter.PARAM_XML_ATTR -> HttpParameterType.XML_ATTRIBUTE
            IParameter.PARAM_MULTIPART_ATTR -> HttpParameterType.MULTIPART_ATTRIBUTE
            else -> HttpParameterType.URL
        }
        val param = HttpParameter.parameter(parameter.name ?: "", parameter.value, paramType)
        val updatedReq = montoyaReq.withAddedParameters(param)
        return updatedReq.toByteArray().getBytes()
    }

    override fun addParameter(request: ByteArray, parameter: IParameter): ByteArray {
        val montoyaReq = HttpRequest.httpRequest(burp.api.montoya.core.ByteArray.byteArray(*request))
        val paramType = when (parameter.type) {
            IParameter.PARAM_URL -> HttpParameterType.URL
            IParameter.PARAM_BODY -> HttpParameterType.BODY
            IParameter.PARAM_COOKIE -> HttpParameterType.COOKIE
            IParameter.PARAM_XML -> HttpParameterType.XML
            IParameter.PARAM_XML_ATTR -> HttpParameterType.XML_ATTRIBUTE
            IParameter.PARAM_MULTIPART_ATTR -> HttpParameterType.MULTIPART_ATTRIBUTE
            else -> HttpParameterType.URL
        }
        val param = HttpParameter.parameter(parameter.name ?: "", parameter.value, paramType)
        val updatedReq = montoyaReq.withAddedParameters(param)
        return updatedReq.toByteArray().getBytes()
    }

    override fun removeParameter(request: ByteArray, parameter: IParameter): ByteArray {
        val montoyaReq = HttpRequest.httpRequest(burp.api.montoya.core.ByteArray.byteArray(*request))
        val paramType = when (parameter.type) {
            IParameter.PARAM_URL -> HttpParameterType.URL
            IParameter.PARAM_BODY -> HttpParameterType.BODY
            IParameter.PARAM_COOKIE -> HttpParameterType.COOKIE
            IParameter.PARAM_XML -> HttpParameterType.XML
            IParameter.PARAM_XML_ATTR -> HttpParameterType.XML_ATTRIBUTE
            IParameter.PARAM_MULTIPART_ATTR -> HttpParameterType.MULTIPART_ATTRIBUTE
            else -> HttpParameterType.URL
        }
        val param = HttpParameter.parameter(parameter.name ?: "", parameter.value, paramType)
        val updatedReq = montoyaReq.withRemovedParameters(param)
        return updatedReq.toByteArray().getBytes()
    }

    override fun analyzeResponseVariations(vararg responses: ByteArray): IResponseVariations {
        TODO("Not yet implemented")
    }

    override fun analyzeResponseKeywords(keywords: List<String>, vararg responses: ByteArray): IResponseKeywords {
        TODO("Not yet implemented")
    }

    override fun getRequestParameter(request: ByteArray, parameterName: String): IParameter {
        TODO("Not yet implemented")
    }

    override fun urlDecode(data: String): String {
        TODO("Not yet implemented")
    }

    override fun urlEncode(data: String): String {
        TODO("Not yet implemented")
    }

    override fun urlDecode(data: ByteArray): ByteArray {
        TODO("Not yet implemented")
    }

    override fun urlEncode(data: ByteArray): ByteArray {
        TODO("Not yet implemented")
    }

    override fun base64Decode(data: String): ByteArray {
        TODO("Not yet implemented")
    }

    override fun base64Decode(data: ByteArray): ByteArray {
        TODO("Not yet implemented")
    }

    override fun base64Encode(data: String): String {
        TODO("Not yet implemented")
    }

    override fun base64Encode(data: ByteArray): String {
        TODO("Not yet implemented")
    }

    override fun indexOf(data: ByteArray, pattern: ByteArray, caseSensitive: Boolean, from: Int, to: Int): Int {
        TODO("Not yet implemented")
    }

    override fun buildHttpMessage(headers: List<String>, body: ByteArray): ByteArray {
        TODO("Not yet implemented")
    }

    override fun buildHttpRequest(url: URL): ByteArray {
        TODO("Not yet implemented")
    }

    override fun toggleRequestMethod(request: ByteArray): ByteArray {
        TODO("Not yet implemented")
    }

    override fun buildHttpService(host: String, port: Int, protocol: String): IHttpService {
        TODO("Not yet implemented")
    }

    override fun buildHttpService(host: String, port: Int, useHttps: Boolean): IHttpService {
        TODO("Not yet implemented")
    }

    override fun makeScannerInsertionPoint(insertionPointName: String, baseRequest: ByteArray, from: Int, to: Int): IScannerInsertionPoint {
        TODO("Not yet implemented")
    }
}

class RequestInfoMontoya(val req: HttpRequest) : IRequestInfo {
    override val method: String
        get() = req.method()

    override val url: URL?
        get() = try { URL(req.url()) } catch (e: Exception) { null }

    private var backingHeaders = req.headers().map { it.toString() }
    override var headers: List<String>
        get() = backingHeaders
        set(value) { backingHeaders = value }

    override val parameters: List<IParameter>
        get() = req.parameters().map {
            val type = when (it.type()) {
                HttpParameterType.URL -> IParameter.PARAM_URL
                HttpParameterType.BODY -> IParameter.PARAM_BODY
                HttpParameterType.COOKIE -> IParameter.PARAM_COOKIE
                HttpParameterType.XML -> IParameter.PARAM_XML
                HttpParameterType.XML_ATTRIBUTE -> IParameter.PARAM_XML_ATTR
                HttpParameterType.MULTIPART_ATTRIBUTE -> IParameter.PARAM_MULTIPART_ATTR
                else -> IParameter.PARAM_URL
            }
            ParameterMontoya(it.name(), it.value(), type)
        }

    override val bodyOffset: Int
        get() = req.bodyOffset()

    override val contentType: Byte
        get() = 0
}

class ResponseInfoMontoya(val res: HttpResponse) : IResponseInfo {
    override val headers: List<String>
        get() = res.headers().map { it.toString() }

    override val bodyOffset: Int
        get() = res.bodyOffset()

    override val statusCode: Short
        get() = res.statusCode()

    override val cookies: List<ICookie>
        get() = emptyList()

    override val statedMimeType: String
        get() = ""

    override val inferredMimeType: String
        get() = ""
}

class ParameterMontoya(
    private var backingName: String,
    private var backingValue: String,
    private val backingType: Byte
) : IParameter {
    override val type: Byte
        get() = backingType

    override val name: String?
        get() = backingName

    override val value: String
        get() = backingValue

    override val nameStart: Int
        get() = -1

    override val nameEnd: Int
        get() = -1

    override val valueStart: Int
        get() = -1

    override val valueEnd: Int
        get() = -1
}

class MessageEditorMontoya(
    val requestEditor: HttpRequestEditor,
    val responseEditor: HttpResponseEditor
) : IMessageEditor {
    private val container = javax.swing.JPanel(java.awt.CardLayout())

    init {
        container.add(requestEditor.uiComponent(), "request")
        container.add(responseEditor.uiComponent(), "response")
    }

    override val component: java.awt.Component
        get() = container

    override fun setMessage(message: ByteArray, isRequest: Boolean) {
        val layout = container.layout as java.awt.CardLayout
        val montoyaBytes = burp.api.montoya.core.ByteArray.byteArray(*message)
        if (isRequest) {
            requestEditor.setRequest(HttpRequest.httpRequest(montoyaBytes))
            layout.show(container, "request")
        } else {
            responseEditor.setResponse(HttpResponse.httpResponse(montoyaBytes))
            layout.show(container, "response")
        }
    }

    override val message: ByteArray
        get() {
            val req = requestEditor.getRequest()
            if (req != null) {
                return req.toByteArray().getBytes()
            }
            val res = responseEditor.getResponse()
            if (res != null) {
                return res.toByteArray().getBytes()
            }
            return ByteArray(0)
        }

    override val isMessageModified: Boolean
        get() = requestEditor.isModified || responseEditor.isModified

    override val selectedData: ByteArray
        get() = ByteArray(0)

    override val selectionBounds: IntArray
        get() = IntArray(2)
}

class HttpListenerMontoya(val legacyListener: IHttpListener) : HttpHandler {
    private fun getToolFlag(toolType: ToolType): Int {
        return when (toolType) {
            ToolType.PROXY -> IBurpExtenderCallbacks.TOOL_PROXY
            ToolType.REPEATER -> IBurpExtenderCallbacks.TOOL_REPEATER
            ToolType.SCANNER -> IBurpExtenderCallbacks.TOOL_SCANNER
            ToolType.INTRUDER -> IBurpExtenderCallbacks.TOOL_INTRUDER
            ToolType.SEQUENCER -> IBurpExtenderCallbacks.TOOL_SEQUENCER
            ToolType.DECODER -> IBurpExtenderCallbacks.TOOL_DECODER
            ToolType.COMPARER -> IBurpExtenderCallbacks.TOOL_COMPARER
            ToolType.TARGET -> IBurpExtenderCallbacks.TOOL_TARGET
            else -> 0
        }
    }

    override fun handleHttpRequestToBeSent(requestToBeSent: HttpRequestToBeSent): RequestToBeSentAction {
        return RequestToBeSentAction.continueWith(requestToBeSent)
    }

    override fun handleHttpResponseReceived(responseReceived: HttpResponseReceived): ResponseReceivedAction {
        val toolFlag = getToolFlag(responseReceived.toolSource().toolType())
        val service = responseReceived.initiatingRequest().httpService()
        val legacyService = SerializableHttpService(
            service.host(),
            service.port().toInt(),
            if (service.secure()) "https" else "http"
        )
        val legacyItem = SerializableHttpRequestResponse(
            responseReceived.initiatingRequest().toByteArray().getBytes(),
            responseReceived.toByteArray().getBytes(),
            null,
            null,
            legacyService
        )
        legacyListener.processHttpMessage(toolFlag, false, legacyItem)
        return ResponseReceivedAction.continueWith(responseReceived)
    }
}

class ContextInvocationMontoya(val event: ContextMenuEvent) : IContextMenuInvocation {
    override val inputEvent: java.awt.event.InputEvent
        get() = java.awt.event.MouseEvent(
            object : java.awt.Component() {},
            java.awt.event.MouseEvent.MOUSE_CLICKED,
            System.currentTimeMillis(),
            0,
            0,
            0,
            0,
            false
        )

    override val selectedMessages: Array<IHttpRequestResponse>?
        get() {
            val messages = event.selectedRequestResponses()
            if (messages.isEmpty()) {
                return null
            }
            return messages.map { item ->
                val service = item.request().httpService()
                val legacyService = SerializableHttpService(
                    service.host(),
                    service.port().toInt(),
                    if (service.secure()) "https" else "http"
                )
                SerializableHttpRequestResponse(
                    item.request().toByteArray().getBytes(),
                    item.response()?.toByteArray()?.getBytes(),
                    null,
                    null,
                    legacyService
                ) as IHttpRequestResponse
            }.toTypedArray()
        }

    override val toolFlag: Int
        get() {
            val name = event.invocationType().name
            return when {
                name.contains("PROXY") -> IBurpExtenderCallbacks.TOOL_PROXY
                name.contains("REPEATER") -> IBurpExtenderCallbacks.TOOL_REPEATER
                name.contains("SCANNER") -> IBurpExtenderCallbacks.TOOL_SCANNER
                name.contains("INTRUDER") -> IBurpExtenderCallbacks.TOOL_INTRUDER
                name.contains("TARGET") -> IBurpExtenderCallbacks.TOOL_TARGET
                else -> 0
            }
        }

    override val invocationContext: Byte
        get() = 0

    override val selectionBounds: IntArray
        get() = IntArray(2)

    override val selectedIssues: Array<IScanIssue>?
        get() = null
}

class ContextMenuFactoryMontoya(val api: MontoyaApi, val legacyFactory: IContextMenuFactory) : ContextMenuItemsProvider {
    override fun provideMenuItems(event: ContextMenuEvent): List<java.awt.Component> {
        val legacyInvocation = ContextInvocationMontoya(event)
        val menuItems = legacyFactory.createMenuItems(legacyInvocation)
        return menuItems ?: emptyList()
    }
}

class BurpCollaboratorClientContextMontoya(val client: burp.api.montoya.collaborator.CollaboratorClient) : IBurpCollaboratorClientContext {
    override val collaboratorServerLocation: String
        get() = client.generatePayload().toString().substringAfter("@", "")

    override fun generatePayload(includeCollaboratorServerLocation: Boolean): String {
        val payload = client.generatePayload()
        return if (includeCollaboratorServerLocation) payload.toString() else payload.toString().substringBefore("@")
    }

    override fun fetchAllCollaboratorInteractions(): List<IBurpCollaboratorInteraction> {
        return emptyList()
    }

    override fun fetchCollaboratorInteractionsFor(payload: String): List<IBurpCollaboratorInteraction> {
        return emptyList()
    }

    override fun fetchAllInfiltratorInteractions(): List<IBurpCollaboratorInteraction> {
        return emptyList()
    }

    override fun fetchInfiltratorInteractionsFor(payload: String): List<IBurpCollaboratorInteraction> {
        return emptyList()
    }
}

class ScanQueueItemMontoya : IScanQueueItem {
    override val status: String get() = "Finished"
    override val percentageComplete: Byte get() = 100
    override val numRequests: Int get() = 0
    override val numErrors: Int get() = 0
    override val numInsertionPoints: Int get() = 0
    override val issues: Array<IScanIssue> get() = emptyArray()
    override fun cancel() {}
}

class HttpRequestResponsePersistedDummy(val item: IHttpRequestResponse) : IHttpRequestResponsePersisted, IHttpRequestResponse by item {
    override fun deleteTempFiles() {}
}
