(function (a) {
    a(function () {
        a(".perc-polls").each(function () {
            a.PercPollRenderer().init(a(this));
        });
    });
    a.PercPollRenderer = function () {
        var d = null;
        var h = null;
        var c = null;
        var f = null;
        var b = { init: k };
        function k(l) {
            d = l;
            h = JSON.parse(d.attr("data-poll"));
            c = d.find(".perc-poll-results-container");
            f = d.find(".perc-poll-render-container");
            d.find(".view-results-link-action a").on("click",function () {
                j();
            });
            d.find(".perc-poll-submit-button, .perc-poll-submit-button-span-action").on("click",function () {
                g();
            });
            d.find("input[name=perc-poll-answer]").on("click",function () {
                d.find(".poll-validation-text").hide();
            });
            if (d.find(".view-results-link-action").length) {
                i(function (m) {
                    if (!m) {
                        j();
                    } else {
                        f.show();
                    }
                });
            }
        }
        function g() {
            var n = d.find("input[name=perc-poll-answer]:checked");
            if (n.length < 1) {
                d.find(".poll-validation-text").show();
                return;
            }
            var m = d.find(".perc-poll-question:first").text();
            var l = {};
            var o = h.restrictionType;
            l.pollName = h.pollName;
            l.pollQuestion = m;
            l.restrictBySession = o == "Session";
            l.pollSubmits = {};
            d.find("input[name=perc-poll-answer]").each(function () {
                var q = a(this).siblings("span").text();
                l.pollSubmits[q] = this.checked;
            });
            var p = "/perc-polls-services/polls/save";
            a.PercServiceUtils.makeXdmJsonRequest(
                null,
                p,
                a.PercServiceUtils.TYPE_POST,
                function (r, t) {
                    if (t.status === a.PercServiceUtils.STATUS_SUCCESS) {
                        try {
                            var u = JSON.parse(t.data);
                        }catch (err){
                            console.error(u.data);
                            return;
                        }
                        if (u.status === "SUCCESS") {
                            if (o === "Cookie") {
                                var q = h.pollName;
                                a.cookie("PercPoll-" + m, "voted", { expires: 7,sameSite: "none",secure:true ,httpOnly:"false"});
                            }
                            var s = u.result;
                            e(s);
                        } else {
                            console.error(u.result);
                        }
                    } else {
                        var v = a.PercServiceUtils.extractDefaultErrorMessage(t.request);
                        console.error(v);
                    }
                },
                l
            );
        }
        function j() {
            var l = d.find(".perc-poll-question:first").text();
            var m = "/perc-polls-services/polls/question/" + encodeURIComponent(l);
            a.PercServiceUtils.makeXdmJsonRequest(null, m, a.PercServiceUtils.TYPE_GET, function (n, p) {
                if (n === a.PercServiceUtils.STATUS_SUCCESS) {
                    var q = JSON.parse(p.data);
                    if (q.status === "SUCCESS") {
                        var o = q.result;
                        e(o);
                    } else {
                        console.error(q.result);
                    }
                } else {
                    var r = a.PercServiceUtils.extractDefaultErrorMessage(p.request);
                    console.error(r);
                }
            });
        }
        function getAnswerVoteCount(pollResults, answerText, renderedAnswers) {
            if (!pollResults || typeof pollResults !== "object") {
                return 0;
            }
            if (Object.prototype.hasOwnProperty.call(pollResults, answerText)) {
                var exact = Number(pollResults[answerText]);
                return !isNaN(exact) && exact > 0 ? exact : 0;
            }
            var target = String(answerText).toLowerCase();
            for (var key in pollResults) {
                if (
                    Object.prototype.hasOwnProperty.call(pollResults, key) &&
                    (!renderedAnswers || !Object.prototype.hasOwnProperty.call(renderedAnswers, key)) &&
                    String(key).toLowerCase() === target
                ) {
                    var count = Number(pollResults[key]);
                    return !isNaN(count) && count > 0 ? count : 0;
                }
            }
            return 0;
        }
        function e(m) {
            f.hide();
            c.show();
            var totalVotes =
                m && typeof m.totalVotes === "number" && !isNaN(m.totalVotes)
                    ? m.totalVotes
                    : parseInt(m && m.totalVotes, 10) || 0;
            if (totalVotes < 0) {
                totalVotes = 0;
            }
            var pollResults = m && m.pollResults ? m.pollResults : {};
            var l = c.find(".perc-poll-result-container").width();
            if (typeof l !== "number" || isNaN(l) || l < 0) {
                l = 0;
            }
            var n = totalVotes > 0 ? l / totalVotes : 0;
            var answerRows = c.find(".perc-poll-answer-container");
            var renderedAnswers = Object.create(null);
            answerRows.each(function () {
                renderedAnswers[a(this).find(".perc-poll-answer").text()] = true;
            });
            answerRows.each(function () {
                var q = a(this);
                var answerText = q.find(".perc-poll-answer").text();
                var s = getAnswerVoteCount(pollResults, answerText, renderedAnswers);
                var t = Math.round(s * n);
                if (t > l) {
                    t = l;
                }
                q.next().find(".perc-poll-result-fill").width(t);
                q.next()
                    .find(".perc-poll-result-empty")
                    .width(l - t);
                var p = totalVotes > 0 ? Math.round((s / totalVotes) * 100) : 0;
                q.find(".perc-poll-result-percent").text(p + "%");
                q.find(".perc-poll-result-count").text("(" + s + "/" + totalVotes + ")");
            });
            c.find(".perc-poll-total-votes span").text(" " + totalVotes);
            d.find(".perc-poll-backto-poll")
                .off("click")
                .on("click", function () {
                    f.show();
                    c.hide();
                });
            i(function (o) {
                if (!o) {
                    d.find(".perc-poll-backto-poll").hide();
                } else {
                    d.find(".perc-poll-backto-poll").show();
                }
            });
        }
        function i(q) {
            var o = h.restrictionType;
            var l = h.pollName;
            var n = d.find(".perc-poll-question:first").text();
            var p = !1;
            if (o === "Unrestricted") {
                q(!0);
            } else {
                if (o === "Cookie") {
                    if (!a.cookie("PercPoll-" + n)) {
                        q(!0);
                    } else {
                        q(!1);
                    }
                } else {
                    if (o === "Session") {
                        var n = d.find(".perc-poll-question:first").text();
                        var m = "/perc-polls-services/polls/canuservote/" + encodeURIComponent(n);
                        a.PercServiceUtils.makeXdmJsonRequest(null, m, a.PercServiceUtils.TYPE_GET, function (r, s) {
                            if (s.status === a.PercServiceUtils.STATUS_SUCCESS) {
                                if (s.data) {
                                    q(!0);
                                } else {
                                    q(!1);
                                }
                            } else {
                                q(!1);
                            }
                        });
                    }
                }
            }
        }
        return b;
    };
})(jQuery);
