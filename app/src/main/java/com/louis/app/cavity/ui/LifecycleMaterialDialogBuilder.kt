package com.louis.app.cavity.ui

import android.content.Context
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.louis.app.cavity.util.L

/**
 * A material dialog that close itself when the registered lifecycle owner reach "on destroy" state
 */
class LifecycleMaterialDialogBuilder(
    context: Context,
    private val lifecycleOwner: LifecycleOwner
) :
    MaterialAlertDialogBuilder(context) {

    override fun setTitle(titleId: Int): MaterialAlertDialogBuilder {
        context.getString(titleId).uppercase().let {
            return super.setTitle(it)
        }
    }

    override fun setTitle(title: CharSequence?): MaterialAlertDialogBuilder {
        return super.setTitle(title.toString().uppercase())
    }

    override fun show(): AlertDialog {
        return super.show().also {
            lifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    it.dismiss()
                }
            })
        }
    }
}
