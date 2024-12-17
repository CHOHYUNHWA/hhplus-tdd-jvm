package io.hhplus.tdd.point;

import io.hhplus.tdd.point.controller.PointController;
import io.hhplus.tdd.point.domain.UserPoint;
import io.hhplus.tdd.point.service.PointService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Spring MVC 테스트 가능
 */
@WebMvcTest(PointController.class)
public class PointControllerTest {

    @MockBean
    PointService pointService;

    @Autowired
    WebApplicationContext ctx;

    static final long USER_ID = 1L;
    MockMvc mvc;

    /**
     * 테스트 실행 전마다 스프링 컨테이너 기반의 컨트롤러,필터,인터셉터를 자동으로 설정
     * alwaysDo(print()) -> 모든 요청/응답에 대한 상세 로그 출력
     */
    @BeforeEach
    public void setup(){
        this.mvc = MockMvcBuilders.webAppContextSetup(ctx)
                .addFilter(new CharacterEncodingFilter("UTF-8", true))
                .alwaysDo(print())
                .build();
    }

    @Test
    @DisplayName("UserId가 숫자가 아닌 경우 조회 실패")
    void failWhenUserIdIsNotANumber() throws Exception {
        //given
        String invalidUserID = "str";

        //when
        //then
        mvc.perform(get("/point/{id}", invalidUserID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("잘못된 요청 값입니다.")));
    }

    @Test
    @DisplayName("UserId가 숫자인 경우 조회 성공")
    void succeedWhenUserIdIsNumeric() throws Exception {
        //given
        UserPoint userPoint = new UserPoint(USER_ID, 0, 0);
        given(pointService.getPoint(USER_ID)).willReturn(userPoint);

        //when
        //then
        mvc.perform(get("/point/{id}", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(USER_ID))
                .andExpect(jsonPath("$.point").value(0))
                .andExpect(jsonPath("$.updateMillis").value(0));
        verify(pointService).getPoint(USER_ID);
    }
}
