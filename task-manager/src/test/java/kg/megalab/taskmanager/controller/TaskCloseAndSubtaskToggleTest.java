package kg.megalab.taskmanager.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Covers the "close/toggle requires a completion comment" rule end-to-end (validation,
 * persistence, and the pre-existing role/department authorization it sits on top of).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TaskCloseAndSubtaskToggleTest {

    @Autowired
    private MockMvc mockMvc;

    private String adminToken;
    private String boardToken;
    private String headToken;
    private String taskId;
    private String subtaskId;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = login("admin", "Passw0rd!");

        String boardId = createUser("Board Tester", "close-test-board", "Passw0rd!", "board");
        String headId = createUser("Head Tester", "close-test-head", "Passw0rd!", "head");
        String deptId = createDepartment("Close Test Dept", boardId, headId);
        patchUserDepartment(headId, deptId);

        boardToken = login("close-test-board", "Passw0rd!");
        headToken = login("close-test-head", "Passw0rd!");

        taskId = createTask(deptId, "Test task");
        subtaskId = createSubtask(taskId, "Test subtask");
    }

    // ---- subtask toggle ----

    @Test
    void markingSubtaskDoneWithoutCommentIsRejected() throws Exception {
        mockMvc.perform(patch("/api/v1/subtasks/{id}/toggle", subtaskId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.field").value("comment"));
    }

    @Test
    void markingSubtaskDoneWithBlankCommentIsRejected() throws Exception {
        mockMvc.perform(patch("/api/v1/subtasks/{id}/toggle", subtaskId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("comment", "   ")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.field").value("comment"));
    }

    @Test
    void markingSubtaskDoneWithNoBodyAtAllIsRejected() throws Exception {
        // toggle's @RequestBody is optional (required=false) to allow un-marking without one —
        // marking done must still fail validation rather than NPE.
        mockMvc.perform(patch("/api/v1/subtasks/{id}/toggle", subtaskId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.field").value("comment"));
    }

    @Test
    void markingSubtaskDoneWithCommentSucceedsAndStoresIt() throws Exception {
        String comment = "Собрали данные, отчёт сформирован и передан в бухгалтерию.";
        mockMvc.perform(patch("/api/v1/subtasks/{id}/toggle", subtaskId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("comment", comment)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.done").value(true))
                .andExpect(jsonPath("$.doneComment").value(comment));
    }

    @Test
    void unmarkingSubtaskDoesNotRequireCommentAndClearsIt() throws Exception {
        markSubtaskDone(subtaskId, "Готово.");

        mockMvc.perform(patch("/api/v1/subtasks/{id}/toggle", subtaskId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.done").value(false))
                .andExpect(jsonPath("$.doneComment").value(nullValue()));
    }

    // ---- task close ----

    @Test
    void closingTaskWithoutCommentIsRejected() throws Exception {
        mockMvc.perform(patch("/api/v1/tasks/{id}/close", taskId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.field").value("comment"));
    }

    @Test
    void closingTaskWithBlankCommentIsRejected() throws Exception {
        mockMvc.perform(patch("/api/v1/tasks/{id}/close", taskId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("comment", "   ")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.field").value("comment"));
    }

    @Test
    void closingTaskWithCommentSucceedsAndStoresIt() throws Exception {
        String comment = "Все договоры сверены, дополнительные соглашения подписаны.";
        mockMvc.perform(patch("/api/v1/tasks/{id}/close", taskId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("comment", comment)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("done"))
                .andExpect(jsonPath("$.progress").value(1.0))
                .andExpect(jsonPath("$.closedAt").exists())
                .andExpect(jsonPath("$.closeComment").value(comment));
    }

    @Test
    void closingTaskAsBoardMemberIsForbidden() throws Exception {
        // /tasks/{id}/close is admin/head-only — the comment requirement sits behind this gate,
        // so it must still hold even with a well-formed comment.
        mockMvc.perform(patch("/api/v1/tasks/{id}/close", taskId)
                        .header("Authorization", "Bearer " + boardToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("comment", "Готово.")))
                .andExpect(status().isForbidden());
    }

    @Test
    void closingTaskAsHeadOfOwnDepartmentSucceeds() throws Exception {
        mockMvc.perform(patch("/api/v1/tasks/{id}/close", taskId)
                        .header("Authorization", "Bearer " + headToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("comment", "Готово.")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("done"));
    }

    // ---- setup helpers ----

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("username", username, "password", password)))
                .andExpect(status().isOk())
                .andReturn();
        return read(result, "$.accessToken");
    }

    private String createUser(String fullName, String login, String password, String role) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("fullName", fullName, "login", login, "password", password, "role", role)))
                .andExpect(status().isOk())
                .andReturn();
        return read(result, "$.id");
    }

    private String createDepartment(String name, String curatorId, String headId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/departments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("name", name, "curatorId", curatorId, "headId", headId)))
                .andExpect(status().isOk())
                .andReturn();
        return read(result, "$.id");
    }

    private void patchUserDepartment(String userId, String departmentId) throws Exception {
        mockMvc.perform(patch("/api/v1/users/{id}", userId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("departmentId", departmentId)))
                .andExpect(status().isOk());
    }

    private String createTask(String departmentId, String title) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("title", title, "departmentId", departmentId,
                                "priority", "high", "initialDeadline", "2026-12-01")))
                .andExpect(status().isOk())
                .andReturn();
        return read(result, "$.id");
    }

    private String createSubtask(String taskId, String title) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/tasks/{id}/subtasks", taskId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("title", title, "deadline", "2026-11-15")))
                .andExpect(status().isOk())
                .andReturn();
        return read(result, "$.id");
    }

    private void markSubtaskDone(String subtaskId, String comment) throws Exception {
        mockMvc.perform(patch("/api/v1/subtasks/{id}/toggle", subtaskId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("comment", comment)))
                .andExpect(status().isOk());
    }

    private static String read(MvcResult result, String path) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), path);
    }

    /** Minimal flat-object JSON builder — avoids Jackson version coupling in this test. */
    private static String json(String... kv) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < kv.length; i += 2) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append('"').append(kv[i]).append("\":");
            String value = kv[i + 1];
            sb.append(value == null ? "null" : '"' + value.replace("\"", "\\\"") + '"');
        }
        return sb.append('}').toString();
    }
}
