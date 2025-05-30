package com.wenjunhuang.codeepiphany.codeforces.actions

import com.wenjunhuang.codeepiphany.model.CodeDojo
import com.wenjunhuang.codeepiphany.utils.actions.UserLoggedIn
import com.wenjunhuang.codeepiphany.utils.competitiveCompanion.CCAction

class CodeForcesCCAction extends CCAction with UserLoggedIn(CodeDojo.CodeForces){
}
