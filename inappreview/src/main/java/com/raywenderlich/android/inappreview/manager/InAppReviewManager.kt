package com.raywenderlich.android.inappreview.manager

import android.app.Activity

/**
 * Abstracts away the implementation behind the In-App Review flow.
 * */
interface InAppReviewManager {

  /**
   * Attempts to start the In-App Review flow.
   * */
  fun startReview(activity: Activity)
}