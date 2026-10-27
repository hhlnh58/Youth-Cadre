const { initializeTestEnvironment, assertFails, assertSucceeds } = require("/usr/local/lib/node_modules/@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

const PROJECT_ID = "demo-no-project";
const DATABASE_ID = "ai-studio-android-youthcad-30b08d2c-ab7e-460a-87fe-c81dc562f8fe";

let testEnv;

before(async () => {
  const rules = fs.readFileSync("firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules: rules,
      host: "127.0.0.1",
      port: 8085,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write tasks", async () => {
  const unauthedDb = testEnv.unauthenticatedContext().firestore({ databaseId: DATABASE_ID });
  const taskRef = unauthedDb.doc("users/user1/tasks/task1");
  await assertFails(taskRef.get());
  await assertFails(taskRef.set({ title: "Hack", userId: "user1", assignedBy: "A", assignee: "B", deadline: "25/10/2026", priority: "Cao", status: "Chưa bắt đầu" }));
});

test("Authenticated user can create, read, update, and delete their own task", async () => {
  const aliceDb = testEnv.authenticatedContext("alice").firestore({ databaseId: DATABASE_ID });
  const taskRef = aliceDb.doc("users/alice/tasks/task1");

  await assertSucceeds(
    taskRef.set({
      userId: "alice",
      title: "Chuẩn bị Đại hội Chi đoàn",
      assignedBy: "Đ/c Bí thư",
      assignee: "Đ/c Nguyễn Văn A",
      coordinator: "Đ/c Lê Thị B",
      deadline: "30/10/2026",
      priority: "Khẩn cấp",
      status: "Đang thực hiện",
      progress: 50,
      notes: ["Đã duyệt văn kiện"],
      attachments: ["vankien.pdf"]
    })
  );

  await assertSucceeds(taskRef.get());

  await assertSucceeds(
    taskRef.update({
      progress: 80
    })
  );

  await assertSucceeds(taskRef.delete());
});

test("User cannot write to another user's task or use mismatched userId", async () => {
  const bobDb = testEnv.authenticatedContext("bob").firestore({ databaseId: DATABASE_ID });
  const aliceTaskRef = bobDb.doc("users/alice/tasks/task1");

  await assertFails(
    aliceTaskRef.set({
      userId: "alice",
      title: "Xâm nhập trái phép",
      assignedBy: "A",
      assignee: "B",
      deadline: "30/10/2026",
      priority: "Thấp",
      status: "Chưa bắt đầu"
    })
  );

  const bobSpoofedRef = bobDb.doc("users/bob/tasks/task1");
  await assertFails(
    bobSpoofedRef.set({
      userId: "alice", // mismatch
      title: "Mạo danh",
      assignedBy: "A",
      assignee: "B",
      deadline: "30/10/2026",
      priority: "Thấp",
      status: "Chưa bắt đầu"
    })
  );
});

test("User can create and read events, circulars, and advisories", async () => {
  const userDb = testEnv.authenticatedContext("user123").firestore({ databaseId: DATABASE_ID });

  // Event
  const eventRef = userDb.doc("users/user123/events/ev1");
  await assertSucceeds(
    eventRef.set({
      userId: "user123",
      title: "Họp Ban Thường vụ Quận Đoàn",
      date: "25/10/2026",
      time: "08:30",
      location: "Hội trường A",
      host: "Đ/c Bí thư",
      attendees: "UV Ban Thường vụ",
      notes: "Mang theo dự thảo báo cáo"
    })
  );

  // Circular
  const circularRef = userDb.doc("users/user123/circulars/doc1");
  await assertSucceeds(
    circularRef.set({
      userId: "user123",
      code: "01-HD/TWĐTN",
      title: "Hướng dẫn tổ chức Đại hội Đoàn các cấp",
      issuer: "Ban Bí thư Trung ương Đoàn",
      category: "Hướng dẫn",
      summary: "Quy định quy trình nhân sự và tổ chức đại hội"
    })
  );

  // Advisory
  const advisoryRef = userDb.doc("users/user123/advisories/adv1");
  await assertSucceeds(
    advisoryRef.set({
      userId: "user123",
      title: "Tờ trình về Kế hoạch Tháng Thanh niên 2026",
      targetOrg: "Ban Dân vận Quận ủy",
      content: "Đề xuất các chương trình tình nguyện vì cộng đồng",
      actionPlan: ["Tuần 1: Lễ ra quân", "Tuần 2: Ngày Chủ nhật xanh"]
    })
  );
});
