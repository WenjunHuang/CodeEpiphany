package com.wenjunhuang.codeepiphany.toolwindows.sidebar.submissionlog

import javax.swing.JComponent

import com.wenjunhuang.codeepiphany.database.tables.records.{LeetcodeSubmissionRecord, SolutionSubmissionRecord}
import com.wenjunhuang.codeepiphany.model.{Language, SubmissionResult}
import com.wenjunhuang.codeepiphany.toolwindows.sidebar.submissionlog.SubmissionLogPresenter.SubmissionType

object LeetCodeSubmissionResultFormHelper {
  def createFromSubmissionType(
    result: SubmissionResult,
    submissionRecord: SolutionSubmissionRecord,
    leetcodeSubmissionRecord: LeetcodeSubmissionRecord
  ): JComponent = {
    result match {
      case SubmissionResult.Success =>
        LeetCodeSuccessResultForm(
          leetcodeSubmissionRecord.getStatusruntime(),
          leetcodeSubmissionRecord.getStatusmemory()
        ).getComponent;
      case SubmissionResult.Failure =>
        new LeetCodeWrongAnswerResultForm(
          leetcodeSubmissionRecord.getInputformatted(),
          leetcodeSubmissionRecord.getCodeoutput(),
          leetcodeSubmissionRecord.getExpectedoutput()
        ).getComponent;
      case _ => LeetCodeErrorResultForm(submissionRecord.getMessage).getComponent;

    }
  }
}
