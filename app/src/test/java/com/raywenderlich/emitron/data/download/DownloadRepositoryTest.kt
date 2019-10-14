package com.raywenderlich.emitron.data.download

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.nhaarman.mockitokotlin2.*
import com.raywenderlich.emitron.data.content.ContentDataSourceLocal
import com.raywenderlich.emitron.data.createContent
import com.raywenderlich.emitron.model.*
import com.raywenderlich.emitron.model.entity.Download
import com.raywenderlich.emitron.utils.TestCoroutineRule
import com.raywenderlich.emitron.utils.async.ThreadManager
import com.raywenderlich.emitron.utils.isEqualTo
import com.raywenderlich.emitron.utils.observeForTestingResult
import kotlinx.coroutines.Dispatchers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.threeten.bp.LocalDateTime
import org.threeten.bp.Month
import java.io.IOException

class DownloadRepositoryTest {

  private val downloadApi: DownloadApi = mock()

  private val threadManager: ThreadManager = mock()

  private val contentDataSourceLocal: ContentDataSourceLocal = mock()

  private lateinit var repository: DownloadRepository

  @get:Rule
  val instantTaskExecutorRule: InstantTaskExecutorRule = InstantTaskExecutorRule()

  @get:Rule
  val testCoroutineRule: TestCoroutineRule = TestCoroutineRule()

  @Before
  fun setUp() {
    whenever(threadManager.io).doReturn(Dispatchers.Unconfined)
    whenever(threadManager.db).doReturn(Dispatchers.Unconfined)
    repository = DownloadRepository(
      downloadApi,
      threadManager,
      contentDataSourceLocal
    )
  }

  @Test
  fun fetchAndSaveContent() {
    testCoroutineRule.runBlockingTest {
      val expectedContent = createContent()
      whenever(downloadApi.getContent("1")).doReturn(
        createContent()
      )
      repository.fetchAndSaveContent("1")
      verify(downloadApi).getContent("1")
      verify(contentDataSourceLocal).insertContent(expectedContent)
      verifyNoMoreInteractions(downloadApi)
      verifyNoMoreInteractions(contentDataSourceLocal)
    }
  }

  @Test
  fun fetchAndSaveContent_ApiError() {
    testCoroutineRule.runBlockingTest {
      whenever(downloadApi.getContent("1")).doThrow(
        IOException()
      )
      repository.fetchAndSaveContent("1")
      verify(downloadApi).getContent("1")
      verifyNoMoreInteractions(contentDataSourceLocal)
      verifyNoMoreInteractions(downloadApi)
    }
  }

  @Test
  fun getQueuedDownloads() {
    testCoroutineRule.runBlockingTest {
      val expected = listOf(
        com.raywenderlich.emitron.data.createDownloadWithContent(),
        com.raywenderlich.emitron.data.createDownloadWithContent()
      )
      whenever(
        contentDataSourceLocal.getQueuedDownloads(
          1,
          states = arrayOf(1, 5),
          contentTypes = ContentType.getAllowedDownloadTypes()
        )
      ).doReturn(
        expected
      )
      val result = repository.getQueuedDownloads(
        1,
        states = arrayOf(DownloadState.CREATED, DownloadState.PAUSED),
        contentTypes = ContentType.getAllowedDownloadTypes()
      )
      result isEqualTo expected
      verify(contentDataSourceLocal).getQueuedDownloads(
        1,
        arrayOf(1, 5),
        arrayOf("screencast", "episode")
      )
      verifyNoMoreInteractions(contentDataSourceLocal)
    }
  }

  @Test
  fun getInProgressDownloads() {
    testCoroutineRule.runBlockingTest {
      val expected = com.raywenderlich.emitron.data.createDownloadWithContent()
      whenever(contentDataSourceLocal.getDownload("1")).doReturn(
        expected
      )
      val result = repository.getDownload("1")

      result isEqualTo expected
      verify(contentDataSourceLocal).getDownload("1")
      verifyNoMoreInteractions(contentDataSourceLocal)
    }
  }

  @Test
  fun addDownload() {
    testCoroutineRule.runBlockingTest {
      val today = LocalDateTime.of(2019, Month.AUGUST, 11, 2, 0, 0)
      repository.addDownload("1", DownloadState.IN_PROGRESS, today)
      verify(contentDataSourceLocal).insertDownload("1", DownloadState.IN_PROGRESS, today)
      verifyNoMoreInteractions(contentDataSourceLocal)
    }
  }

  @Test
  fun removeDownload() {
    testCoroutineRule.runBlockingTest {
      repository.removeDownload(listOf("1"))
      verify(contentDataSourceLocal).deleteDownload(listOf("1"))
      verifyNoMoreInteractions(contentDataSourceLocal)
    }
  }

  @Test
  fun removeAllDownloads() {
    testCoroutineRule.runBlockingTest {
      repository.removeAllDownloads()
      verify(contentDataSourceLocal).deleteAllDownloads()
      verifyNoMoreInteractions(contentDataSourceLocal)
    }
  }

  @Test
  fun getDownloadUrl() {
    testCoroutineRule.runBlockingTest {
      val expected = Contents(
        datum = listOf(
          Data(
            attributes = Attributes(
              kind = "hd_video_file",
              url = "download/hd"
            )
          ),
          Data(
            attributes = Attributes(
              kind = "sd_video_file",
              url = "download/sd"
            )
          )
        )
      )
      whenever(downloadApi.getDownloadUrl("1")).doReturn(expected)
      val result = repository.getDownloadUrl("1")
      result isEqualTo expected
      verify(downloadApi).getDownloadUrl("1")
      verifyNoMoreInteractions(contentDataSourceLocal)
    }
  }

  @Test
  fun updateDownloadUrl() {
    testCoroutineRule.runBlockingTest {
      repository.updateDownloadUrl("1", "download/1")
      verify(contentDataSourceLocal).updateDownloadUrl("1", "download/1")
      verifyNoMoreInteractions(contentDataSourceLocal)
    }
  }

  @Test
  fun updateDownloadProgress() {
    testCoroutineRule.runBlockingTest {
      repository.updateDownloadProgress("1", 25, DownloadState.IN_PROGRESS)
      verify(contentDataSourceLocal)
        .updateDownloadProgress("1", 25, DownloadState.IN_PROGRESS)
      verifyNoMoreInteractions(contentDataSourceLocal)
    }
  }

  @Test
  fun updateDownloadState() {
    testCoroutineRule.runBlockingTest {
      repository.updateDownloadState("1", DownloadState.COMPLETED)
      verify(contentDataSourceLocal)
        .updateDownloadState("1", DownloadState.COMPLETED)
      verifyNoMoreInteractions(contentDataSourceLocal)
    }
  }

  @Test
  fun getDownloadsById() {
    testCoroutineRule.runBlockingTest {

      val expected = listOf(
        Download(
          "1",
          "download/1",
          25,
          0,
          0,
          "createdAt"
        ),
        Download(
          "1",
          "download/1",
          25,
          0,
          0,
          "createdAt"
        )
      )
      whenever(repository.getDownloadsById(listOf("1", "2"))).doReturn(
        MutableLiveData<List<Download>>().apply {
          value = expected
        }
      )
      val downloads = repository.getDownloadsById(listOf("1", "2"))
      val result = downloads.observeForTestingResult()
      result isEqualTo expected
      verify(contentDataSourceLocal)
        .getDownloadsById(listOf("1", "2"))
      verifyNoMoreInteractions(contentDataSourceLocal)
    }
  }
}
