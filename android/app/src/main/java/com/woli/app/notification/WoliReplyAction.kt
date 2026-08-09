package com.woli.app.notification

data class WoliReplyAction(
    val actionId: String,
    val eventId: String,
    val label: String,
    val requiresUnlock: Boolean,
)

sealed class WoliReplySendResult {
    data object Sent : WoliReplySendResult()
    data object EmptyText : WoliReplySendResult()
    data object NoReplyAction : WoliReplySendResult()
    data object Expired : WoliReplySendResult()
    data object Canceled : WoliReplySendResult()
    data class Failed(val reason: String) : WoliReplySendResult()

    val isSuccess: Boolean
        get() = this is Sent

    fun userMessage(): String {
        return when (this) {
            Sent -> "답장 전송 요청을 완료했어요."
            EmptyText -> "빈 답장은 보낼 수 없습니다."
            NoReplyAction -> "이 알림은 답장을 지원하지 않습니다."
            Expired -> "답장 액션이 만료되었습니다. 새 알림에서 다시 시도하세요."
            Canceled -> "상대 앱의 답장 액션이 취소되었습니다."
            is Failed -> reason
        }
    }
}
