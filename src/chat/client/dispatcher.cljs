(ns chat.client.dispatcher
  (:require [chat.client.store :as store]
            [chat.client.sync :as sync]
            [chat.client.schema :as schema]))

(defmulti dispatch! (fn [event data] event))

(defmethod dispatch! :new-message [_ data]
  (let [message (schema/make-message {:user-id (get-in @store/app-state [:session :user-id])
                                      :content (data :content)
                                      :thread-id (data :thread-id)})]
    (store/add-message! message)
    (sync/chsk-send! [:chat/new-message message])))

(defmethod dispatch! :hide-thread [_ data]
  (sync/chsk-send! [:chat/hide-thread (data :thread-id)])
  (store/transact! [:users (get-in @store/app-state [:session :user-id]) :hidden-thread-ids] #(conj % (data :thread-id))))


(defmethod dispatch! :seed [_ _]
  (reset! store/app-state
          {:session {:user-id 1}
           :users {1 {:id 1
                      :icon "https://s3-us-west-2.amazonaws.com/slack-files2/avatars/2015-05-08/4801271456_41230ac0b881cdf85c28_72.jpg"
                      :hidden-thread-ids []}
                   2 {:id 2
                      :icon "https://s3-us-west-2.amazonaws.com/slack-files2/avatars/2015-05-09/4805955000_07dc272f0a7f9de7719e_192.jpg"
                      :hidden-thread-ids []}}
           :messages {99 {:id 99 :content "Hello?" :thread-id 123 :user-id 1 :created-at (js/Date. 1)}
                      98 {:id 98 :content "Hi!" :thread-id 123 :user-id 2 :created-at (js/Date. 2)}
                      97 {:id 97 :content "Oh, great, someone else is here." :thread-id 123 :user-id 1 :created-at (js/Date. 3)}
                      96 {:id 96 :content "Yep" :thread-id 123 :user-id 2 :created-at (js/Date. 4)}}}))

(defmethod sync/event-handler :chat/new-message
  [[_ data]]
  (store/add-message! data))