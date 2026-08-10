package com.woli.app.voice

enum class WoliVoiceCommandType {
    StartReply,
    SendReply,
    CancelReply,
    AnswerCall,
    DeclineCall,
    ReadRemainingTime,
    ContinueFocus,
    CompleteFocus,
    StartUnlockMission,
    Unknown,
}

data class WoliVoiceCommand(
    val type: WoliVoiceCommandType,
    val rawText: String,
) {
    val isKnown: Boolean
        get() = type != WoliVoiceCommandType.Unknown
}

object WoliVoiceCommandParser {
    fun parse(text: String): WoliVoiceCommand {
        val normalized = text
            .lowercase()
            .replace(Regex("\\s+"), "")

        val type = when {
            normalized.isBlank() -> WoliVoiceCommandType.Unknown
            normalized.hasAny("답장", "답해", "회신") &&
                !normalized.hasAny("보내", "전송") -> WoliVoiceCommandType.StartReply
            normalized.hasAny("보내", "전송", "확인", "응답완료") -> WoliVoiceCommandType.SendReply
            normalized.hasAny("취소", "지워", "다시", "그만") -> WoliVoiceCommandType.CancelReply
            normalized.hasAny("전화받", "받아", "수락", "통화") -> WoliVoiceCommandType.AnswerCall
            normalized.hasAny("거절", "끊어", "나중", "무시") -> WoliVoiceCommandType.DeclineCall
            normalized.hasAny("남은시간", "시간알려", "몇분", "얼마나남") -> WoliVoiceCommandType.ReadRemainingTime
            normalized.hasAny("계속", "집중계속", "돌아가") -> WoliVoiceCommandType.ContinueFocus
            normalized.hasAny("완료", "끝났", "종료완료") -> WoliVoiceCommandType.CompleteFocus
            normalized.hasAny("해제", "풀어", "미션", "잠금해제") -> WoliVoiceCommandType.StartUnlockMission
            else -> WoliVoiceCommandType.Unknown
        }

        return WoliVoiceCommand(type = type, rawText = text.trim())
    }

    private fun String.hasAny(vararg keywords: String): Boolean {
        return keywords.any(::contains)
    }
}
