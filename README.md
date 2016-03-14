# README #

This README would normally document whatever steps are necessary to get your application up and running.

### Android application for OTP communication ###

* Quick summary
This will be the repo with all the code relating to the android app.

### How do I get set up? ###

* Download android studio (http://developer.android.com/sdk/index.html)
* Clone into the repository and open it using android studio
* Recommended
** In android studio click VCS->enable version control and then choose git 

## Design Flow

### Creating a Conversation

- User clicks add floating action button, moves to `NewConversationActivity`
- User enters parameters, including name of contact and OTP size
- User clicks create
- Activity switches to `CreatingNoteboookFragment`, calls `OTPManager` for new OTPs
- `OTPManager` finishes creating OTPs, activity creates new `Person`, `OTP` and `Conversation` objects to set up conversation
- Activity moves to `SyncMethodActivity`
- User selects one of the sync methods, activity moves to the selected activity
- Selected activity is given `OTP` ids to sync
- Activity does what it needs to. When done, moves to `ConversationActivity` giving `Conversation` id

### Conversation messaging

- User arrives at a `ConversationActivity`
- `ConversationActivity` refreshes OTPs to get new messages, adds any messages received
- User enters a message and clicks send
- `ConversationActivity` sends message, and adds sent message