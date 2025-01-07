package com.wenjunhuang.codeepiphany.toolwindows.sidebar.submissionlog

import com.wenjunhuang.codeepiphany.model.Difficulty
import com.wenjunhuang.codeepiphany.model.CodeDojo

case class SubmissionLogEntry(dojo: CodeDojo, challengeTitle: String, solution: String, difficulty: Difficulty)
