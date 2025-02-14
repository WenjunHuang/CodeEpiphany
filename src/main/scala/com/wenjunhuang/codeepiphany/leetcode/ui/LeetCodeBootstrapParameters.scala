package com.wenjunhuang.codeepiphany.leetcode.ui

import com.wenjunhuang.codeepiphany.leetcode.models.*

case class LeetCodeBootstrapParameters(
  userInfo: LeetCodeUserInfo,
  categories: List[LeetCodeCategoryListItem] = Nil,
  favorites: List[LeetCodeFavoriteItem] = Nil,
  tagTypeWithTags: List[LeetCodeTagTypeWithTags] = Nil,
  companies: List[LeetCodeQuestionCompanyTag] = Nil,
  positions: List[LeetCodeProblemsetPositionTag] = Nil
)
