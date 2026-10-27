package com.example.data.repository

import androidx.test.core.app.ApplicationProvider
import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.FirestoreEvent
import com.example.data.model.FirestoreTask
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class FirestoreRepositoryRuleTest : FirestoreEmulatorTestBase() {

  private lateinit var repository: FirestoreCadreRepository

  @Before
  override fun setUpFirebase() {
    super.setUpFirebase()
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    repository = FirestoreCadreRepository(context, firestore)
  }

  @Test
  fun authenticatedUser_canCreateAndObserveTasks() = runBlocking {
    val uid = signInTestUser("cadre_alice@example.com")

    val task = FirestoreTask(
      userId = uid,
      title = "Chuẩn bị Đại hội Đoàn",
      description = "Duyệt văn kiện chính trị",
      assignedBy = "Đ/c Bí thư",
      assignee = "Đ/c Nguyễn Văn A",
      coordinator = "Đ/c Lê Thị B",
      deadline = "28/10/2026",
      priority = "Khẩn cấp",
      status = "IN_PROGRESS",
      progress = 50L
    )

    repository.saveTask(task)

    val tasks = repository.observeTasks().first()
    assertTrue("Nhiệm vụ phải được lưu thành công trên Firestore", tasks.any { it.title == "Chuẩn bị Đại hội Đoàn" })
  }

  @Test
  fun authenticatedUser_canCreateAndObserveEvents() = runBlocking {
    val uid = signInTestUser("cadre_bob@example.com")

    val event = FirestoreEvent(
      userId = uid,
      title = "Họp Thường trực Đoàn",
      description = "Rà soát Tháng Thanh niên",
      date = "26/10/2026",
      time = "08:30",
      location = "Phòng họp số 1",
      host = "Đ/c Bí thư",
      attendees = "Thường trực Thành Đoàn"
    )

    repository.saveEvent(event)

    val events = repository.observeEvents().first()
    assertTrue("Lịch công tác phải được lưu thành công", events.any { it.title == "Họp Thường trực Đoàn" })
  }
}
