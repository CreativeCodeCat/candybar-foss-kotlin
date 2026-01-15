package candybar.lib.items

import android.content.ComponentName

class Request private constructor(
    val name: String,
    val activity: String
) {
    private var _packageName: String? = null
    var packageName: String?
        get() {
            if (_packageName == null) {
                if (activity.isNotEmpty() && activity.contains("/")) {
                    return activity.substring(0, activity.lastIndexOf("/"))
                }
            }
            return _packageName
        }
        set(value) {
            _packageName = value
        }

    var orderId: String? = null
    var productId: String? = null
    var requestedOn: String? = null
    var iconBase64: String? = null
    var isRequested: Boolean = false
    var isAvailableForRequest: Boolean = false
    var infoText: String? = null
    var fileName: String? = null

    class Builder {
        private var name: String = ""
        private var activity: String = ""
        private var packageName: String? = null
        private var orderId: String? = null
        private var productId: String? = null
        private var requestedOn: String? = null
        private var isRequested: Boolean = false
        private var isAvailableForRequest: Boolean = true
        private var infoText: String = ""

        fun name(name: String) = apply { this.name = name }
        fun activity(activity: String) = apply { this.activity = activity }
        fun packageName(packageName: String?) = apply { this.packageName = packageName }
        fun orderId(orderId: String?) = apply { this.orderId = orderId }
        fun productId(productId: String?) = apply { this.productId = productId }
        fun requestedOn(requestedOn: String?) = apply { this.requestedOn = requestedOn }
        fun requested(requested: Boolean) = apply { this.isRequested = requested }
        fun availableForRequest(available: Boolean) = apply { this.isAvailableForRequest = available }
        fun infoText(infoText: String) = apply { this.infoText = infoText }

        fun build(): Request {
            return Request(name, activity).apply {
                packageName = this@Builder.packageName
                requestedOn = this@Builder.requestedOn
                isRequested = this@Builder.isRequested
                orderId = this@Builder.orderId
                productId = this@Builder.productId
                isAvailableForRequest = this@Builder.isAvailableForRequest
                infoText = this@Builder.infoText
            }
        }
    }

    class Property(
        var componentName: ComponentName?,
        val orderId: String?,
        val productId: String?
    )

    companion object {
        @JvmStatic
        fun Builder(): Builder = Builder()
    }
}
