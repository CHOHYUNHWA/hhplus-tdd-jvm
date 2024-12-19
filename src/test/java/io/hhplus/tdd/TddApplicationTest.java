package io.hhplus.tdd;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

/**
 * E2E 통합테스트
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@AutoConfigureMockMvc
public class TddApplicationTest {

    @Autowired
    private MockMvc mvc;

    static final long USER_ID = 1L;
    static final long AMOUNT = 1000L;

    @Test
    @DisplayName("0원 미만의 포인트 충전 요청 시 실패")
    void failToChargeWhenAmountIsBelowZero() throws Exception {
        long negativeAmount = -100L; // 음수 충전 금액

        mvc.perform(patch("/point/{id}/charge", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(negativeAmount)))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(containsString("충천할 포인트는 0보다 커야 합니다.")));
    }


    @Test
    @DisplayName("충전 시 유저 포인트 합계가 100만 초과 시 실패")
    void failToChargeWhenTotalPointsExceedMaxLimit() throws Exception {
        long currentPoint = 1L;
        long chargeAmount = 1_000_000L;

        mvc.perform(patch("/point/{id}/charge", USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.valueOf(currentPoint)));

        mvc.perform(patch("/point/{id}/charge", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(chargeAmount)))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(containsString("최대 충전할 수 있는 포인트는 100만 입니다.")));
    }

    @Test
    @DisplayName("유저 포인트 충전 요청 시 성공")
    void succeedWhenChargingUserPoints() throws Exception {
        mvc.perform(patch("/point/{id}/charge", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(AMOUNT)))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(USER_ID))
                .andExpect(MockMvcResultMatchers.jsonPath("$.point").value(AMOUNT));
    }

    @Test
    @DisplayName("0원 미만의 포인트 사용 요청 시 실패")
    void failToUsePointsWhenAmountIsBelowZero()throws Exception {
        long negativeAmount = -100L; // 음수 충전 금액
        mvc.perform(patch("/point/{id}/use", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(negativeAmount)))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("사용할 포인트는 0 이상이어야 합니다."));

    }

    @Test
    @DisplayName("사용 시 유저 포인트 보다 사용 포인트 초과 시 실패")
    void failToUsePointsWhenAmountExceedsAvailablePoints() throws Exception {
        long currentPoint = 1000L;
        long useAmount = 1500L;
        mvc.perform(patch("/point/{id}/charge", USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.valueOf(currentPoint)));

        mvc.perform(patch("/point/{id}/use", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(useAmount)))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("사용할 포인트는 보유 포인트보다 작아야 합니다."));
    }

    @Test
    @DisplayName("유저 포인트 사용 요청 시 성공")
    void succeedWhenUsingUserPoints() throws Exception {
        long currentPoint = 1000L;
        long useAmount = 500L;
        mvc.perform(patch("/point/{id}/charge", USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.valueOf(currentPoint)));

        mvc.perform(patch("/point/{id}/use", USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.valueOf(useAmount)))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(USER_ID))
                .andExpect(MockMvcResultMatchers.jsonPath("$.point").value(currentPoint - useAmount));
    }

    @Test
    @DisplayName("유저의 포인트 조회 성공")
    void succeedWhenFetchingUserPoints() throws Exception {
        mvc.perform(patch("/point/{id}/charge", USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.valueOf(AMOUNT)));

        mvc.perform(get("/point/{id}", USER_ID))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(USER_ID))
                .andExpect(MockMvcResultMatchers.jsonPath("$.point").value(AMOUNT));
    }

    @Test
    @DisplayName("유저의 포인트 충전/사용 내역 조회 성공")
    void succeedWhenFetchingUserPointHistory() throws Exception{
        mvc.perform(patch("/point/{id}/charge", USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.valueOf(AMOUNT)));
        mvc.perform(patch("/point/{id}/charge", USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.valueOf(AMOUNT)));
        mvc.perform(patch("/point/{id}/use", USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.valueOf(AMOUNT)));

        mvc.perform(get("/point/{id}/histories", USER_ID))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$", hasSize(3)))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].userId").value(USER_ID))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].amount").value(AMOUNT))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].type").value("CHARGE"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].userId").value(USER_ID))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].amount").value(AMOUNT))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].type").value("CHARGE"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[2].userId").value(USER_ID))
                .andExpect(MockMvcResultMatchers.jsonPath("$[2].amount").value(AMOUNT))
                .andExpect(MockMvcResultMatchers.jsonPath("$[2].type").value("USE"));
    }
}
