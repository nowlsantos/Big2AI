package com.nowlsantos.big2ai.presentation.event

sealed class UserActionEvent {
    object PlayCardsClicked : UserActionEvent()
    object PassTurnClicked : UserActionEvent()
    object SortByRankClicked : UserActionEvent()
    object SortBySuitClicked : UserActionEvent()
}
