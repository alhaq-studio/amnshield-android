# Privacy Policy for DeenShield

**Last Updated: 2025-10-29**

## 1. Introduction

Welcome to DeenShield ("we," "our," "us"). We are committed to protecting your privacy and providing a transparent understanding of how we handle your data. DeenShield is designed with a "privacy-first" approach. Our core mission is to help you build healthier digital habits, and that includes respecting your personal information.

This Privacy Policy explains what information we collect, how we use it, and your choices regarding your information. The fundamental principle of DeenShield is that **all sensitive data processing happens entirely on your device.** We do not have servers that collect your personal activity data.

## 2. Information We Collect

To provide our features, DeenShield collects information in the following ways:

### a. Information You Provide to Us

*   **Account Information:** When you choose to sign in with your Google Account, we receive your basic profile information, such as your name and email address, as provided by Google. This is used solely for authentication and managing premium features.
*   **Configuration Data:** You provide us with your preferences and settings, such as lists of blocked applications, blocked keywords, focus mode configurations, and other settings you configure within the app. This data is stored locally on your device.

### b. Information Collected Automatically Through App Usage

This is the most sensitive category of data, and we want to be exceptionally clear about it. To perform its functions, DeenShield uses Android's Accessibility Services and other permissions. This data is **processed exclusively on your device** and is **never transmitted to our servers or any third party.**

*   **Application Usage Data:**
    *   We monitor the applications you use (`packageName`) to enforce your app blocking and focus mode rules.
    *   We use Android's `UsageStatsManager` API to gather aggregated screen time for different apps to display usage statistics to you within the app.
*   **On-Screen Content (for Keyword and Smart Features):**
    *   **Keyword Blocker:** The Accessibility Service analyzes text appearing on your screen in real-time to identify and block keywords that you have defined.
    *   **Smart Blur / Smart Content Moderation (Premium Feature):** For this feature, the Accessibility Service analyzes on-screen text and images. This content is passed to an on-device machine learning model (TensorFlow Lite) to detect potentially inappropriate content (e.g., nudity, suggestive content).
    *   **Crucially, all this analysis happens locally.** The text, images, and videos from other apps never leave your device.
*   **Crash Logs:** The application generates logs if it crashes. These logs contain information about the error but do not contain personal identifiers. We only have access to this information if you **explicitly choose** to share it with us via the "Share Crash Log" feature.

## 3. Justification for Permissions

We require certain permissions to provide the features of DeenShield. Here is a detailed explanation of why we need each of the key permissions:

*   **Accessibility Service:** This is the core permission that allows DeenShield to function. We use it to:
    *   Detect the app you are currently using to enforce app blocking and focus mode.
    *   Read the text on your screen to identify and block keywords.
    *   Analyze on-screen content for the Smart Blur feature.
    *   **We do not log or store any of this data.** It is processed in real-time on your device and then discarded.
*   **Package Usage Stats:** This permission allows us to access your app usage history. We use this to:
    *   Show you detailed statistics about your app usage.
    *   **This data is stored locally on your device and is not transmitted to our servers.**
*   **System Alert Window (Overlay):** This permission allows us to display content over other apps. We use this to:
    *   Show the Smart Blur overlay when inappropriate content is detected.
    *   Display the warning screen when a blocked app or view is accessed.
*   **Device Administrator:** This permission is optional and is only required for the Anti-Uninstall feature. It allows us to:
    *   Prevent the app from being uninstalled without your permission.
    *   **This permission does not give us access to any of your personal data.**
*   **VPN Service:** This permission is used for the DNS-based filtering feature. It allows us to:
    *   Filter network traffic to block ads and trackers.
    *   **We do not log or monitor your network traffic.** All filtering is done on your device.

## 4. How We Use Your Information

We use the information we collect solely to provide and improve the functionality of the DeenShield app.

*   **To Provide Core Features:** Your configuration data and the data from the Accessibility Service are used to make the app, view, and keyword blockers work as you've configured them.
*   **To Display Your Stats:** App usage data is used to generate the charts and statistics you see on the "Stats" screen.
*   **To Authenticate and Manage Your Account:** Your Google Account information is used to sign you in and verify your premium status.
*   **For On-Device Smart Features:** Text and image data are used locally by the on-device machine learning models to provide smart content moderation.
*   **To Improve the App:** Anonymized crash logs, which you voluntarily share, help us diagnose and fix bugs.

## 5. How We Do NOT Use Your Information (On-Device Processing)

To reiterate and emphasize our commitment to your privacy:
*   We **DO NOT** transmit your app usage history, blocked keywords, or any on-screen content to our servers.
*   We **DO NOT** sell or share your personal data with third-party advertisers or data brokers.
*   We **DO NOT** have the ability to remotely view your screen or access your personal files.

## 6. Data Sharing and Third Parties

DeenShield is designed to minimize data sharing. We do not share your personal information, except in the limited circumstances described below:

*   **Google Sign-In:** If you sign in, you are sharing information directly with Google, subject to Google's Privacy Policy. We only receive a token to verify your identity.
*   **Google Play Billing:** If you purchase a premium subscription, the transaction is processed by the Google Play Store. We do not collect or store your payment card information.
*   **Legal Requirements:** We may disclose information if required to do so by law or in the good faith belief that such action is necessary to comply with a legal obligation.

## 7. Data Retention and Deletion

All your personal configuration and usage data is stored locally on your device's internal storage. This data is retained as long as you have the DeenShield app installed.

You have full control over your data. You can delete your data in two ways:
1.  **Clearing App Data:** You can go to your device's Settings -> Apps -> DeenShield -> Storage and tap "Clear Data." This will permanently delete all data associated with the app.
2.  **Uninstalling the App:** Uninstalling the DeenShield application will remove the app and all of its associated data from your device.

## 8. Your Rights and Choices

You have the following rights regarding your information:
*   **Access and Modify:** You can access and modify your configuration data at any time within the app's settings.
*   **Disable Permissions:** You can disable the Accessibility Service or other permissions for DeenShield at any time through your device's settings. Please note that doing so will cause the core features of the app to stop working.
*   **Opt-out of Account Sync:** You can use the app without signing into a Google account, though premium features may require it.

## 9. Security

We take reasonable measures to protect the information stored locally on your device. The app is built on standard Android platform security features. However, no method of electronic storage is 100% secure, especially as the data resides on your personal device, the security of which is outside our control.

## 10. Children's Privacy

DeenShield is not intended for use by children under the age of 13 (or the relevant age in your jurisdiction). We do not knowingly collect personal information from children.

## 11. Changes to This Privacy Policy

We may update our Privacy Policy from time to time. We will notify you of any changes by posting the new Privacy Policy within the app and updating the "Last Updated" date. You are advised to review this Privacy Policy periodically for any changes.

## 12. Contact Us

If you have any questions or concerns about this Privacy Policy, please contact us at:
**contact@alhaq-initiative.org**
