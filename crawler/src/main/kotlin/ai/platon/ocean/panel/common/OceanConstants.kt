package ai.platon.ocean.panel.common

import ai.platon.pulsar.common.AppContext
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

val EPOCH_LDT = LocalDateTime.ofInstant(Instant.EPOCH, ZoneId.systemDefault())!!

const val PROP_DOWNLOAD_LAST_PAGE_NUMBER = "downloadLastPageNumber"

const val PROP_DOWNLOAD_LAST_PAGE_SIZE = "downloadLastPageSize"

const val PROP_FETCH_NEXT_OFFSET = "fetchNextOffset"

const val DOWNLOAD_PAGE_SIZE = 50

const val FETCH_LIMIT = 100

val isDevelopment = !AppContext.HOST_NAME.contains("platonai")
