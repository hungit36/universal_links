import Flutter
import UIKit

private let kMessagesChannel = "uni_links/messages"
private let kEventsChannel = "uni_links/events"

public class UniversalLinksPlugin: NSObject, FlutterPlugin, FlutterStreamHandler {
    private var initialLink: String?
    private var latestLink: String?
    private var eventSink: FlutterEventSink?
    
    private static var instance: UniversalLinksPlugin?
    
    public static func sharedInstance() -> UniversalLinksPlugin {
        if instance == nil {
            instance = UniversalLinksPlugin()
        }
        return instance!
    }
    
    public static func register(with registrar: FlutterPluginRegistrar) {
        let instance = UniversalLinksPlugin.sharedInstance()
        
        let channel = FlutterMethodChannel(name: kMessagesChannel, binaryMessenger: registrar.messenger())
        registrar.addMethodCallDelegate(instance, channel: channel)
        
        let eventChannel = FlutterEventChannel(name: kEventsChannel, binaryMessenger: registrar.messenger())
        eventChannel.setStreamHandler(instance)
        
        registrar.addApplicationDelegate(instance)
    }
    
    private func setLatestLink(_ link: String?) {
        latestLink = link
        if let eventSink = eventSink {
            eventSink(latestLink)
        }
    }
    
    public func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        if let url = launchOptions?[.url] as? URL {
            initialLink = url.absoluteString
            latestLink = initialLink
        }
        return true
    }
    
    public func application(_ app: UIApplication, open url: URL, options: [UIApplication.OpenURLOptionsKey : Any] = [:]) -> Bool {
        setLatestLink(url.absoluteString)
        return true
    }
    
    public func application(_ application: UIApplication, continue userActivity: NSUserActivity, restorationHandler: @escaping ([UIUserActivityRestoring]?) -> Void) -> Bool {
        if userActivity.activityType == NSUserActivityTypeBrowsingWeb, let url = userActivity.webpageURL {
            setLatestLink(url.absoluteString)
            if eventSink == nil {
                initialLink = latestLink
            }
            return true
        }
        return false
    }
    
    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        if call.method == "getInitialLink" {
            result(initialLink)
        } else {
            result(FlutterMethodNotImplemented)
        }
    }
    
    public func onListen(withArguments arguments: Any?, eventSink: @escaping FlutterEventSink) -> FlutterError? {
        self.eventSink = eventSink
        return nil
    }
    
    public func onCancel(withArguments arguments: Any?) -> FlutterError? {
        self.eventSink = nil
        return nil
    }
}

