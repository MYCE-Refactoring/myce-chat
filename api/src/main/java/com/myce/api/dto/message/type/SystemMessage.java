package com.myce.api.dto.message.type;

public final class SystemMessage {

    public static final String NEW_CHAT = "새로운 대화입니다.";
    public static final String AI_INVITE_MESSAGE = "찍찍! 상담원을 찾고 있어요. 잠시만 기다려주세요! 그동안 다른 궁금한 점이 있으시면 언제든 말씀해주세요.";
    public static final String AI_HANDOFF_MESSAGE = "AI가 상담을 이어받습니다. 언제든 도움이 필요하시면 말씀해주세요.";
    public static final String AI_RETURN_MESSAGE = "찍찍! 다시 제가 도와드리게 되었어요. 어떤 도움이 필요하신가요?";
    public static final String CANCEL_HANDOFF = "찍찍! 상담원 연결 요청을 취소했어요. 제가 계속 도와드리겠습니다!";
    public static final String NOT_EXIST_SUMMARY_MESSAGE = "찍찍! 대화 내용이 없어 요약할 내용이 없습니다.";
    public static final String SUCCESS_ADMIN_HANDOFF = "관리자가 상담에 참여했습니다.찍찍\n더 자세하고 전문적인 도움을 드리겠습니다.";
    public static final String PERMISSION_DENIED_ADMIN_CHAT = "상담 권한이 없습니다. 현재 담당자: %s";
    public static final String USE_HAND_OFF_MESSAGE = "AI 상담 중에는 직접 메시지를 보낼 수 없습니다. '개입하기' 버튼을 사용해주세요.";

    public static final String ERROR_REQUEST_ADMIN_HANDOFF = "관리자 연결 요청에 실패했습니다.";
    public static final String ERROR_CANCEL_ADMIN_HANDOFF = "관리자 연결 취소 요청을 실패했습니다.";
    public static final String ERROR_PROACTIVE_INTERVENTION = "관리자 사전 개입에 실패했스니다.";
    public static final String ERROR_ACCEPT_HANDOFF = "관리자 인계 수락에 실패했습니다.";
    public static final String ERROR_REQUEST_AI_HANDOFF = "AI 연결 요청에 실패했습니다.";
}
