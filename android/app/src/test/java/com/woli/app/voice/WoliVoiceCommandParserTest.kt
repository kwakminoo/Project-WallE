package com.woli.app.voice

import org.junit.Assert.assertEquals
import org.junit.Test

class WoliVoiceCommandParserTest {
    @Test
    fun parsesReplyCommands() {
        assertEquals(
            WoliVoiceCommandType.StartReply,
            WoliVoiceCommandParser.parse("월이야 답장해줘").type,
        )
        assertEquals(
            WoliVoiceCommandType.SendReply,
            WoliVoiceCommandParser.parse("보내").type,
        )
        assertEquals(
            WoliVoiceCommandType.CancelReply,
            WoliVoiceCommandParser.parse("취소해").type,
        )
    }

    @Test
    fun parsesCallAndFocusCommands() {
        assertEquals(
            WoliVoiceCommandType.AnswerCall,
            WoliVoiceCommandParser.parse("전화 받아").type,
        )
        assertEquals(
            WoliVoiceCommandType.DeclineCall,
            WoliVoiceCommandParser.parse("거절해").type,
        )
        assertEquals(
            WoliVoiceCommandType.ReadRemainingTime,
            WoliVoiceCommandParser.parse("남은 시간 알려줘").type,
        )
    }
}
