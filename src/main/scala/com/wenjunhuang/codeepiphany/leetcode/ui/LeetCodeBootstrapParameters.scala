package com.wenjunhuang.codeepiphany.leetcode.ui

import com.wenjunhuang.codeepiphany.leetcode.model.{LeetCodeCategoryListItem, LeetCodeFavoriteItem, LeetCodeTagTypeWithTags, LeetCodeUserInfo}

case class LeetCodeBootstrapParameters(
  userInfo: LeetCodeUserInfo,
  categories: List[LeetCodeCategoryListItem] = Nil,
  favorites: List[LeetCodeFavoriteItem] = Nil,
  tagTypeWithTags: List[LeetCodeTagTypeWithTags] = Nil
)
