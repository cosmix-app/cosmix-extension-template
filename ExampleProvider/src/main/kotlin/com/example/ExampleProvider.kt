package com.example

import com.cosmix.app.CsxApi
import com.cosmix.app.SearchResponse
import com.cosmix.app.TvType

class ExampleProvider : CsxApi() { // All providers must be an instance of CsxApi
    override var mainUrl = "https://example.com/" 
    override var name = "Example provider"
    override val supportedTypes = setOf(TvType.Movie)

    override var lang = "en"

    // Enable this when your provider has a main page
    override val hasMainPage = true

    // This function gets called when you search for something
    override suspend fun search(query: String): List<SearchResponse> {
        return listOf()
    }
}
