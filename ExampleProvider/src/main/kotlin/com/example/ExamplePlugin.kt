package com.example

import com.cosmix.app.plugins.CsxPlugin
import com.cosmix.app.plugins.CsxPluginAnnotation

@CsxPluginAnnotation
class ExamplePlugin: CsxPlugin() {
    override fun load() {
        // All providers should be added in this manner
        registerCsxApi(ExampleProvider())
    }
}
