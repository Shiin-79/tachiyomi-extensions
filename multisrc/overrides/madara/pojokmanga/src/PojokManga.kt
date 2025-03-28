package eu.kanade.tachiyomi.extension.id.pojokmanga

import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.Locale

class PojokManga : Madara("Pojok Manga", "https://pojokmanga.net", "id", SimpleDateFormat("MMM dd, yyyy", Locale.US)) {

    override val useLoadMoreSearch = false

    override val useNewChapterEndpoint = true

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        var url = "$baseUrl/${searchPage(page)}".toHttpUrlOrNull()!!.newBuilder()
        url.addQueryParameter("s", query)
        url.addQueryParameter("post_type", "wp-manga")
        filters.forEach { filter ->
            when (filter) {
                is AuthorFilter -> {
                    if (filter.state.isNotBlank()) {
                        url.addQueryParameter("author", filter.state)
                    }
                }
                is ArtistFilter -> {
                    if (filter.state.isNotBlank()) {
                        url.addQueryParameter("artist", filter.state)
                    }
                }
                is YearFilter -> {
                    if (filter.state.isNotBlank()) {
                        url.addQueryParameter("release", filter.state)
                    }
                }
                is StatusFilter -> {
                    filter.state.forEach {
                        if (it.state) {
                            url.addQueryParameter("status[]", it.id)
                        }
                    }
                }
                is OrderByFilter -> {
                    if (filter.state != 0) {
                        url.addQueryParameter("m_orderby", filter.toUriPart())
                    }
                }
                is AdultContentFilter -> {
                    url.addQueryParameter("adult", filter.toUriPart())
                }
                is GenreConditionFilter -> {
                    url.addQueryParameter("op", filter.toUriPart())
                }
                is GenreList -> {
                    filter.state
                        .filter { it.state }
                        .let { list ->
                            if (list.isNotEmpty()) { list.forEach { genre -> url.addQueryParameter("genre[]", genre.id) } }
                        }
                }
                is ProjectFilter -> {
                    if (filter.toUriPart() == "project-filter-on") {
                        url = "$baseUrl/project/page/$page".toHttpUrlOrNull()!!.newBuilder()
                    }
                }
            }
        }
        return GET(url.toString(), headers)
    }

    override fun searchMangaSelector() = "div.c-tabs-item__content, div.page-item-detail"

    protected class ProjectFilter : UriPartFilter(
        "Filter Project",
        arrayOf(
            Pair("Show all manga", ""),
            Pair("Show only project manga", "project-filter-on")
        )
    )

    override fun getFilterList(): FilterList {
        val filters = super.getFilterList().toMutableList()

        filters += listOf(
            Filter.Separator(),
            Filter.Header("NOTE: cant be used with other filter!"),
            Filter.Header("$name Project List page"),
            ProjectFilter()
        )

        return FilterList(filters)
    }
}
