#!/usr/bin/env bb

(ns https-terminator
  (:require
   [clojure.string :as str]
   [clojure.pprint :refer [pprint]]
   [org.httpkit.client :as http]
   [org.httpkit.server :as server]
   [org.httpkit.sni-client :as sni-client]
   [clojure.tools.cli :refer [parse-opts]]))

(alter-var-root #'org.httpkit.client/*default-client* (fn [_] sni-client/default-client))

(defn call-remote [method remote-root-url uri body headers query-string] 
  @(http/request {:method method
                  :url (str remote-root-url uri (when query-string (str "?" query-string)))
                  :headers (->> headers
                                (remove (fn [[k _]]
                                          (#{"host"} k))) 
                                (into {}))
                  :body body}))
 
(defn handler [req remote-root-url verbose]
  (let [req-timestamp (java.util.Date.)
        {:keys [uri body headers request-method query-string]} req
        req-body-str (slurp body) 
        {res-status :status
         res-body :body
         res-headers :headers} (call-remote request-method remote-root-url uri req-body-str headers query-string)
        res-timestamp (java.util.Date.)] 
    (when verbose
      (println keys req)
      (println "\n========================= REQ =========================\n")
      (pprint
       {:timestamp    req-timestamp
        :method       request-method
        :uri          uri
        :query-string query-string
        :headers      headers})  
      (println (str "\n" req-body-str))
      (println "\n========================= RES =========================\n")
      (pprint
       {:timestamp   res-timestamp
        :res-headers res-headers})
      (println (str "\n" res-body)))
    {:status res-status
     :body res-body
     :headers (into {} (map (fn [[k v]] [(str k) v]) res-headers))}))

(defn start-server [port remote-root-url verbose]
  (server/run-server (fn [req] (#'handler req remote-root-url verbose)) 
   {:port port}))

(defn print-app-purporse []
  (println
   "https_terminator.clj
    
Spawns a simple HTTP server which proxies all incoming HTTP requests to remote HTTPS server, preserving relative path, headers and url params.\n"))

(def cli-options
  [["-t" "--target-url TARGETURL" "Url of remote HTTPS server. Should not end with /."
    :validate [(fn [v]
                 (and (not (str/blank? v))
                      (not (str/ends-with? v "/"))))
               "Must be a non-empty URL not ending with /"]]
   ["-p" "--port PORT" "localhost port for newly spawned HTTP server"
    :parse-fn parse-long
    :validate [some? "Must be a valid numeric port number"]] 
   ["-v" "--verbose" "Print verbose logging messages (eg. content of request/response)"]
   ["-h" "--help" "Shows this usage information."]])

(defn print-sample-usage []
  (println "\n\nexample: bb https_terminator.clj -t https://some.remoteserver.com -p 1234"))

(let [{:keys [options summary errors]} (parse-opts *command-line-args* cli-options)
      {:keys [target-url port help verbose]} options]
  (cond
    (or help (empty? *command-line-args*))
    (do (print-app-purporse)
        (println summary)
        (print-sample-usage)
        (System/exit 0))

    errors
    (do (println "Errors in command line arguments: " errors)
        (System/exit 1))

    :else
    (do (start-server port target-url verbose)
        (println (format "HTTP server running on port %s, redirecting calls to %s"
                         port target-url))
        @(promise))))
