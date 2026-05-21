package ch.pentagrid.burpexts.pentagridscancontroller

import burp.api.montoya.BurpExtension
import burp.api.montoya.MontoyaApi
import burp.BurpExtenderCallbacksMontoya

class PentagridScanControllerExtension : BurpExtension {

    override fun initialize(api: MontoyaApi) {
        val callbacks = BurpExtenderCallbacksMontoya(api)
        BurpExtender().registerExtenderCallbacks(callbacks)
    }

}
