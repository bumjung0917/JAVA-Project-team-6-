
package llm;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Scanner;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.Gson;

public class SunmoonChat {

    public static void main(String[] args) throws Exception {

        // 1. API 키 가져오기
        String apiKey = System.getenv("SUNMOON_API_KEY");

        if (apiKey == null || apiKey.isBlank()) {
            System.out.println("API 키를 먼저 설정하세요.");
            return;
        }

        // 2. AI 시스템 프롬프트
        String systemPrompt = """
        너는 프로젝트 관리 전용 AI 챗봇이다.

        다음 4가지 기능만 수행한다.

        1. 자동 업무 분배
        2. 보고서 초안 생성
        3. 프로젝트 예상 질문 생성
        4. 작업 시작일 추천

        [1. 자동 업무 분배]
        - 프로젝트에 등록된 업무와 팀원 정보를 분석한다.
        - 팀원의 담당 역할과 업무량을 고려한다.
        - 필요한 정보가 부족하면 사용자에게 질문한다.

        [2. 보고서 초안 생성]
        - 프로젝트 진행 상황과 업무 완료 현황을 바탕으로 작성한다.
        - 프로젝트명, 진행률, 완료 및 미완료 업무 등을 정리한다.

        [3. 프로젝트 예상 질문 생성]
        - 프로젝트 진행 상황과 등록된 업무를 바탕으로 예상 질문을 생성한다.
        - 프로젝트 발표와 중간 점검에 적합한 질문을 작성한다.

        [4. 작업 시작일 추천]
        - 업무 마감일, 예상 소요 시간, 팀원 일정을 고려한다.
        - 추천 날짜와 추천 이유를 설명한다.

        [답변 제한]
        - 프로젝트와 관련 없는 질문에는 답변하지 않는다.
        - 위 4가지 기능에 해당하지 않는 요청은 수행하지 않는다.
        - 지원하지 않는 요청에는 다음과 같이 안내한다.
        "해당 요청은 현재 지원하지 않는 기능입니다."

        [데이터 활용]
        - 전달받은 실제 프로젝트 데이터를 우선 활용한다.
        - 현재 DB는 연결되어 있지 않다.
        - 사용자가 제공하지 않은 프로젝트, 팀원, 업무, 일정 정보를
          임의로 만들어 내지 않는다.
        - 필요한 정보가 부족하면 사용자에게 추가 정보를 요청한다.
        - 예시 데이터를 실제 프로젝트 데이터처럼 표현하지 않는다.

        [답변 규칙]
        - 한국어로 답변한다.
        - 초보자도 이해하기 쉽게 설명한다.
        - 번호와 목록을 활용하여 정리한다.
        - 불필요한 설명은 최소화한다.
        """;

        // 3. HTTP 클라이언트 생성
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();

        Gson gson = new Gson();

        // 4. 대화 기록 생성
        JsonArray messages = new JsonArray();

        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content", systemPrompt);

        messages.add(systemMessage);

        // 5. 사용자 질문 입력
        Scanner scanner = new Scanner(System.in);

        System.out.println("================================");
        System.out.println("     프로젝트 관리 AI 챗봇");
        System.out.println("================================");
        System.out.println("지원 기능:");
        System.out.println("1. 자동 업무 분배");
        System.out.println("2. 보고서 초안 생성");
        System.out.println("3. 프로젝트 예상 질문 생성");
        System.out.println("4. 작업 시작일 추천");
        System.out.println("--------------------------------");
        System.out.println("종료하려면 exit를 입력하세요.");

        while (true) {

            System.out.print("\n사용자: ");
            String question = scanner.nextLine();

            if (question.equalsIgnoreCase("exit")) {
                System.out.println("챗봇을 종료합니다.");
                break;
            }

            if (question.isBlank()) {
                System.out.println("질문을 입력하세요.");
                continue;
            }

            // 사용자 메시지 추가
            JsonObject userMessage = new JsonObject();
            userMessage.addProperty("role", "user");
            userMessage.addProperty("content", question);

            messages.add(userMessage);

            // 6. API 요청 데이터 생성
            JsonObject requestData = new JsonObject();

            requestData.addProperty("model", "gemma-4-26b");
            requestData.add("messages", messages);
            requestData.addProperty("max_tokens", 1000);
            requestData.addProperty("stream", false);

            String requestBody = gson.toJson(requestData);

            // 7. API 요청 생성
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                        "https://llm.deepaibit.com/v1/chat/completions"))
                    .timeout(Duration.ofSeconds(120))
                    .header("Authorization", "Bearer " + apiKey.trim())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                        requestBody, StandardCharsets.UTF_8))
                    .build();

            try {

                // 8. AI 응답 받기
                HttpResponse<String> response = client.send(
                        request,
                        HttpResponse.BodyHandlers.ofString(
                            StandardCharsets.UTF_8));

                if (response.statusCode() < 200
                        || response.statusCode() >= 300) {

                    System.out.println("API 요청 실패: HTTP "
                            + response.statusCode());
                    System.out.println(response.body());

                    messages.remove(messages.size() - 1);
                    continue;
                }

                // 9. JSON 데이터 분석
                JsonObject result = JsonParser
                        .parseString(response.body())
                        .getAsJsonObject();

                String answer = result
                        .getAsJsonArray("choices")
                        .get(0)
                        .getAsJsonObject()
                        .getAsJsonObject("message")
                        .get("content")
                        .getAsString();

                // AI 답변을 대화 기록에 추가
                JsonObject assistantMessage = new JsonObject();
                assistantMessage.addProperty("role", "assistant");
                assistantMessage.addProperty("content", answer);

                messages.add(assistantMessage);

                // 10. AI 답변 출력
                System.out.println("\nAI: " + answer);

            } catch (Exception e) {

                System.out.println("오류가 발생했습니다: "
                        + e.getMessage());

                messages.remove(messages.size() - 1);
            }
        }

        scanner.close();
    }
}
