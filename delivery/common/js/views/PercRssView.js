/*
 * Copyright 1999-2023 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

(function($) {

    $(document).ready(function() {
        $.PercRssView.updateRssFeed();
    });
    
    $.PercRssView = {
        updateRssFeed : updateRssFeed
    };

    //Escapes a value so it can be safely used as html text or in an attribute value
    function escapeHtml(value)
    {
        var s = ("undefined" === typeof (value) || null === value) ? "" : String(value);
        return $('<div>').text(s).html().replace(/"/g, '&quot;');
    }

    //Only allows simple element names, falls back to the given default
    function safeElem(name, defaultName)
    {
        return ("string" === typeof (name) && /^[a-zA-Z][a-zA-Z0-9]*$/.test(name)) ? name : defaultName;
    }

    //Blocks script bearing urls
    function safeUrl(url)
    {
        var u = ("undefined" === typeof (url) || null === url) ? "" : String(url);
        if (/^\s*(?:javascript|vbscript|data)\s*:/i.test(u)) {return "#";}
        return escapeHtml(u);
    }

    var HTML_NAMED_ENTITIES = {
        amp: "&",
        lt: "<",
        gt: ">",
        quot: "\"",
        apos: "'",
        nbsp: "\u00A0",
        copy: "\u00A9",
        reg: "\u00AE",
        trade: "\u2122",
        mdash: "\u2014",
        ndash: "\u2013",
        hellip: "\u2026",
        lsquo: "\u2018",
        rsquo: "\u2019",
        ldquo: "\u201C",
        rdquo: "\u201D",
        bull: "\u2022",
        middot: "\u00B7",
        laquo: "\u00AB",
        raquo: "\u00BB",
        times: "\u00D7",
        divide: "\u00F7",
        deg: "\u00B0",
        cent: "\u00A2",
        pound: "\u00A3",
        euro: "\u20AC",
        yen: "\u00A5",
        sect: "\u00A7",
        para: "\u00B6"
    };

    function isEnabled(value)
    {
        if ("boolean" === typeof value)
        {
            return value;
        }
        if ("string" === typeof value)
        {
            return "true" === value.toLowerCase();
        }
        return Boolean(value);
    }

    function codePointToString(codePoint)
    {
        if (isNaN(codePoint) || codePoint <= 0 || codePoint > 0x10FFFF || (codePoint >= 0xD800 && codePoint <= 0xDFFF))
        {
            return "\uFFFD";
        }
        if (codePoint <= 0xFFFF)
        {
            return String.fromCharCode(codePoint);
        }
        var cp = codePoint - 0x10000;
        return String.fromCharCode(0xD800 + (cp >> 10), 0xDC00 + (cp & 0x3FF));
    }

    function decodeHtmlEntities(text)
    {
        if (!text || text.indexOf("&") === -1)
        {
            return text;
        }
        return text.replace(/&(?:#x([0-9a-fA-F]{1,8})|#([0-9]{1,8})|([a-zA-Z][a-zA-Z0-9]{0,31}));/g, function(match, hex, dec, named) {
            if (hex)
            {
                return codePointToString(parseInt(hex, 16));
            }
            if (dec)
            {
                return codePointToString(parseInt(dec, 10));
            }
            var key = named.toLowerCase();
            if (Object.prototype.hasOwnProperty.call(HTML_NAMED_ENTITIES, key))
            {
                return HTML_NAMED_ENTITIES[key];
            }
            return match;
        });
    }

    function isAsciiLetter(ch)
    {
        return (ch >= "a" && ch <= "z") || (ch >= "A" && ch <= "Z");
    }

    function isTagNameChar(ch)
    {
        return isAsciiLetter(ch) || (ch >= "0" && ch <= "9") || ch === "-" || ch === ":" || ch === "_";
    }

    function isWhitespace(ch)
    {
        return ch === " " || ch === "\t" || ch === "\n" || ch === "\r" || ch === "\f";
    }

    function skipRawTextElement(s, startIndex, tagName)
    {
        var lower = s.toLowerCase();
        var closePrefix = "</" + tagName;
        var pos = startIndex;
        while (pos < s.length)
        {
            var closeIdx = lower.indexOf(closePrefix, pos);
            if (closeIdx === -1)
            {
                return s.length;
            }
            var afterName = closeIdx + closePrefix.length;
            if (afterName < s.length)
            {
                var nextCh = lower.charAt(afterName);
                if (nextCh === ">" || nextCh === "/" || isWhitespace(nextCh))
                {
                    var endTagIdx = lower.indexOf(">", afterName);
                    return endTagIdx === -1 ? s.length : endTagIdx + 1;
                }
            }
            pos = closeIdx + 2;
        }
        return s.length;
    }

    //Extracts plain text from an HTML fragment without touching DOM HTML sinks
    function stripHtml(value)
    {
        var s = ("undefined" === typeof (value) || null === value) ? "" : String(value);
        var out = [];
        var i = 0;
        var len = s.length;

        while (i < len)
        {
            var lt = s.indexOf("<", i);
            if (lt === -1)
            {
                out.push(s.substring(i));
                break;
            }
            if (lt > i)
            {
                out.push(s.substring(i, lt));
            }
            i = lt;
            if (i + 1 >= len)
            {
                out.push("<");
                break;
            }

            var next = s.charAt(i + 1);

            if (next === "!" && s.substring(i, i + 4) === "<!--")
            {
                if (s.substring(i, i + 5) === "<!-->")
                {
                    i += 5;
                    continue;
                }
                if (s.substring(i, i + 6) === "<!--->")
                {
                    i += 6;
                    continue;
                }
                var commentEnd = s.indexOf("-->", i + 4);
                i = commentEnd === -1 ? len : commentEnd + 3;
                continue;
            }

            if (next === "!")
            {
                if (s.substring(i, i + 9) === "<![CDATA[")
                {
                    var cdataEnd = s.indexOf("]]>", i + 9);
                    i = cdataEnd === -1 ? len : cdataEnd + 3;
                    continue;
                }
                var declEnd = s.indexOf(">", i + 2);
                i = declEnd === -1 ? len : declEnd + 1;
                continue;
            }

            if (next === "?")
            {
                var piEnd = s.indexOf(">", i + 2);
                i = piEnd === -1 ? len : piEnd + 1;
                continue;
            }

            var isClosing = (next === "/");
            var nameStart = isClosing ? i + 2 : i + 1;

            if (isClosing && nameStart < len && s.charAt(nameStart) === ">")
            {
                i = nameStart + 1;
                continue;
            }

            if (nameStart >= len || !isAsciiLetter(s.charAt(nameStart)))
            {
                out.push("<");
                i++;
                continue;
            }

            var nameEnd = nameStart + 1;
            while (nameEnd < len && isTagNameChar(s.charAt(nameEnd)))
            {
                nameEnd++;
            }
            var tagName = s.substring(nameStart, nameEnd).toLowerCase();

            var j = nameEnd;
            var quote = null;
            var expectAttrValue = false;
            while (j < len)
            {
                var ch = s.charAt(j);
                if (quote !== null)
                {
                    if (ch === quote)
                    {
                        quote = null;
                    }
                }
                else
                {
                    if (ch === ">")
                    {
                        break;
                    }
                    if (expectAttrValue)
                    {
                        if (ch === "\"" || ch === "'")
                        {
                            quote = ch;
                            expectAttrValue = false;
                        }
                        else if (!isWhitespace(ch))
                        {
                            expectAttrValue = false;
                        }
                    }
                    else if (ch === "=")
                    {
                        expectAttrValue = true;
                    }
                }
                j++;
            }

            if (j >= len)
            {
                break;
            }

            i = j + 1;
            if (!isClosing && ("script" === tagName || "style" === tagName || "noscript" === tagName))
            {
                i = skipRawTextElement(s, i, tagName);
            }
        }

        return decodeHtmlEntities(out.join(""));
    }
    
    function updateRssFeed()
    {
        $('.perc-feed-widget').each(function(){
            var currentFeedWidget = $(this);
            if ("" === currentFeedWidget.attr("data-query")){ return;}
            var queryString = JSON.parse(currentFeedWidget.attr("data-query") );
            
            $.PercRssService.getExternalFeed(queryString.urlFeed, queryString, function(status, result){
                var feed = new JFeed(result);
                
                if(queryString.showTitle && null !== feed.title && "undefined" !== typeof (feed.title))
                {
                    var feedTitleId = queryString.feedId +"_title";
                    currentFeedWidget.attr('aria-labelledby', feedTitleId);
                    var titleElement = safeElem(queryString.titleElement, "h2");
                    var feedTitle = '<' + titleElement + ' id="' + escapeHtml(feedTitleId) + '" ';
                    feedTitle += ' title="' + escapeHtml(feed.title) + '" class="perc-feed-title"> <a href="' + safeUrl(feed.link) + '" target="_blank" rel="noopener noreferrer">' + escapeHtml(feed.title) + '</a> </'+titleElement+'>';
                    currentFeedWidget.append(feedTitle);
                } else  if (null !== feed.title && "undefined" !== typeof (feed.title)) {
                    currentFeedWidget.attr('aria-label', feed.title);
                } else {
                    currentFeedWidget.attr('aria-label', queryString.feedId);
                }

                var itemLimit = parseInt(queryString.itemLimit.substring(queryString.itemLimit.indexOf("_") + 1));

                var feedItem = '';
                if("undefined" !== typeof feed.items && null !== feed.items) {
                    if (0 > itemLimit)
                    {
                        itemLimit=feed.items.length;
                    }
                    feedItem = renderFeedItems(feed.items, queryString, itemLimit);
                } else {
                    // Handle no items or error here
                }
                currentFeedWidget.append(feedItem);
                currentFeedWidget.attr("aria-busy","false");
            });
        });
    }
    
    //Render feed items
    function renderFeedItems(feedItems, queryString, itemLimit) {
        var feedItem = '';
        var itemAriaRole = queryString.itemAriaRole;
        var wrappingElement = safeElem(queryString.wrappingElement, 'none');
        var itemElement = safeElem(queryString.itemElement, 'div');
        var itemTitleElement = safeElem(queryString.itemTitleElement, 'h3');
        var itemDateElement = safeElem(queryString.itemDateElement, 'p');
        var itemDescriptionElement = safeElem(queryString.itemDescriptionElement, 'p');
        var itemIdPrefix = queryString.feedId +"_item_";
        var wrappingElemAriaLabel = queryString.wrappingElemAriaLabel;

        if ('none' !== wrappingElement)
        {
            feedItem += '<' +wrappingElement;
            if ("ul" !== wrappingElement && "li" !== wrappingElement)
            {
                 feedItem += ' role="list" ';
            }
            if (null !== wrappingElemAriaLabel && "undefined" !== typeof (wrappingElemAriaLabel) && ("" !== wrappingElemAriaLabel)) {
                feedItem += ' aria-label="' + escapeHtml(wrappingElemAriaLabel) + '" >';
            }else {
                feedItem += ' >';
            }
        }
        for(var i = 0; i < feedItems.length && i < itemLimit; i++)
        {
            var item = feedItems[i];
            var id=itemIdPrefix+i;
            var title_id = id+"_title";
            var label="";

            if(queryString.showItemTitle && null !== item.title)
            {
                label='aria-labelledby="' + escapeHtml(title_id) + '"';
            }
            else
            {
                label='aria-label="' + escapeHtml(item.title) + '"';
            }

            feedItem += '<' +itemElement+' ';
            if ("li" !== itemElement && "none" !== wrappingElement)
            {
                feedItem += 'role="listitem" ';
            }

            feedItem += 'id="'+escapeHtml(id)+'" class="perc-feed-item"' + label + '>';
            
            
            if(queryString.showItemTitle && item.title)
            {
                feedItem += '<'+itemTitleElement+' id="'+escapeHtml(title_id)+'" class="perc-feed-item-title"><a href="' + safeUrl(item.link) + '" target="_blank" title="' + escapeHtml(item.title) + '" rel="noopener noreferrer">' + escapeHtml(item.title) + '</a> </'+itemTitleElement+'>';
            }
            
            if(queryString.showItemDate && item.updated)
            {
                var formattedDate = "";
                var formattedTime = "";
                var date = new Date(item.updated);
                
                formattedDate = $.datepicker.formatDate(queryString.itemDateFormat, date);
                formattedDate = formattedDate.replace('hh', date.getHours());
                formattedDate = formattedDate.replace('nn', date.getMinutes());

                feedItem += '<'+itemDateElement+' class="perc-feed-item-date">' + escapeHtml(formattedDate) + '</'+itemDateElement+'>';
            }
            
            if(queryString.showItemDescription && item.description)
            {
                var descriptionText = String(item.description);

                if(isEnabled(queryString.itemRemoveHtml))
                {
                    descriptionText = stripHtml(descriptionText);
                }

                var maxLen = parseInt(queryString.itemDescriptionLength, 10);
                if(!isEnabled(queryString.itemDescriptionEmpty) && !isNaN(maxLen) && maxLen >= 0)
                {
                    //Truncate description
                    var truncated = descriptionText.substring(0, maxLen);
                    //If there are more description
                    if(descriptionText.length > maxLen)
                    {
                        descriptionText = truncated.trim() + "...";
                    }
                    else
                    {
                        descriptionText = truncated;
                    }
                }

                feedItem += '<'+itemDescriptionElement+' class="perc-feed-item-description">' + escapeHtml(descriptionText) + '</'+itemDescriptionElement+'>';
            }
            feedItem += '</'+itemElement+'>';
        }
        if ('none' !== wrappingElement)
        {
            feedItem += '</' +wrappingElement+ '>';
        }
        return feedItem;
    }
    
})(jQuery);
