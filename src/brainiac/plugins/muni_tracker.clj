(ns brainiac.plugins.muni-tracker
  (:require [brainiac.plugin :as brainiac]
            [brainiac.xml-utils :as xml]
            [clojure.contrib.zip-filter.xml :as zf]))

(defn route-with-departures [route]
  (assoc {}
    :name (zf/xml1-> route (zf/attr :Name))
    :direction (zf/xml1-> route :RouteDirectionList :RouteDirection (zf/attr :Name))
    :departures (zf/xml-> route :RouteDirectionList :RouteDirection :StopList :Stop :DepartureTimeList :DepartureTime zf/text)))

(defn has-departures? [route]
  (seq (:departures route)))

(defn transform [stream]
  (let [xml-zipper (xml/parse-xml stream)
        routes (filter has-departures? (zf/xml-> xml-zipper :AgencyList :Agency :RouteList :Route route-with-departures))
        stop (zf/xml1-> xml-zipper :AgencyList :Agency :RouteList :Route :RouteDirectionList :RouteDirection :StopList :Stop (zf/attr :name))]
    (assoc {}
      :name "muni-tracker"
      :stop stop
      :routes routes)))

(defn url [api-key stop-id]
  (format "http://services.my511.org/Transit2.0/GetNextDeparturesByStopCode.aspx?token=%s&stopcode=%s" api-key stop-id))

(defn configure [{:keys [api-key stop-id program-name]}]
  (brainiac/schedule
    20000
    (brainiac/simple-http-plugin
      {:method "GET" :url (url api-key stop-id)}
      transform program-name)))

