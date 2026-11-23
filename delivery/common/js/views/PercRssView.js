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

    //Strips HTML markup safely via inert DOMParser without executing scripts or event handlers
    function stripHtml(value)
    {
        var s = ("undefined" === typeof (value) || null === value) ? "" : String(value);
        if ("" === s) {return "";}
        if ("undefined" !== typeof (DOMParser))
        {
            var doc = new DOMParser().parseFromString(s, "text/html");
            if (doc && doc.body)
            {
                if ("function" === typeof (doc.body.querySelectorAll))
                {
                    var blocked = doc.body.querySelectorAll("script, style");
                    for (var i = 0; i < blocked.length; i++)
                    {
                        if (blocked[i].parentNode)
                        {
                            blocked[i].parentNode.removeChild(blocked[i]);
                        }
                    }
                }
                return doc.body.textContent || "";
            }
            return "";
        }
        return s;
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
                var descText = String(item.description);

                if(true === queryString.itemRemoveHtml || "true" === queryString.itemRemoveHtml)
                {
                    descText = stripHtml(descText);
                }

                if(!queryString.itemDescriptionEmpty)
                {
                    var maxLen = parseInt(queryString.itemDescriptionLength, 10);
                    if(!isNaN(maxLen) && maxLen >= 0 && descText.length > maxLen)
                    {
                        descText = descText.substring(0, maxLen).trim() + "...";
                    }
                }

                feedItem += '<'+itemDescriptionElement+' class="perc-feed-item-description">' + escapeHtml(descText) + '</'+itemDescriptionElement+'>';
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
