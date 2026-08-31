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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.percussion.widgets.image.web.impl;

      import com.fasterxml.jackson.core.JsonProcessingException;
      import com.fasterxml.jackson.databind.JsonNode;
      import com.fasterxml.jackson.databind.ObjectMapper;
      import java.io.PrintWriter;
      import java.util.Iterator;
      import java.util.Map;
      import java.util.Set;
      import javax.servlet.http.HttpServletRequest;
      import javax.servlet.http.HttpServletResponse;
      import org.springframework.web.servlet.View;
      import org.springframework.web.servlet.view.AbstractView;
      import org.springframework.web.util.WebUtils;

      public class JSONView extends AbstractView
        implements View
      {
      String contentType = "text/plain";
      String htmlContentType = "text/html";
      String modelObjectName = null;

        protected void renderMergedOutputModel(Map model, HttpServletRequest request, HttpServletResponse response)
          throws Exception
        {
        boolean debug = WebUtils.hasSubmitParameter(request, "debug");
        boolean isHtml = WebUtils.hasSubmitParameter(request, "forceHTML");
        String requestedWith = request.getHeader("HTTP_X_REQUESTED_WITH");
        boolean jQueryFormFile = WebUtils.hasSubmitParameter(request, "jQueryFormFile");

        PrintWriter pw = response.getWriter();
        response.setContentType(isHtml ? this.htmlContentType : this.contentType);

        if (isHtml)
          {
          pw.write("<html><head><title>ImageResult</title></head>");
          pw.write("<body><textarea rows=\"33\" cols=\" 100\">");
       } else if (jQueryFormFile)
          {
          pw.write("<textarea>");
          response.setContentType(this.htmlContentType);
          }

        if (this.modelObjectName != null)
          {
          JsonNode json = (JsonNode)model.get(this.modelObjectName);
          if (debug)
            pw.write(prettyPrint(json));
            else
            pw.write(json.toString());
          }
          else
          {
          Iterator itr = model.entrySet().iterator();
          while (itr.hasNext())
            {
            Object member = itr.next();
            if ((member instanceof JsonNode))
              {
              JsonNode json = (JsonNode)member;
              if (debug)
                pw.write(prettyPrint(json));
                else {
                pw.write(json.toString());
                }
              }
            }
          }
        if (isHtml)
          {
          pw.write("</textarea></body></html>");
        } else if (jQueryFormFile)
          {
          pw.write("</textarea>");
          }
        pw.flush();
       response.flushBuffer();
        }

        public String getContentType()
        {
        	return this.contentType;
        }

        public void setContentType(String contentType)
        {
        	this.contentType = contentType;
        }

        public String getModelObjectName()
        {
        	return this.modelObjectName;
        }

        public void setModelObjectName(String modelObjectName)
        {
        	this.modelObjectName = modelObjectName;
        }

        /**
         * Returns a pretty-printed JSON string of the given node. T2.17 migration (issue #139):
         * the previous json-lib {@code toString(3)} produced a 3-space indent; Jackson's default
         * pretty printer uses 2 spaces. The indentation is a developer-mode affordance only; the
         * actual JSON shape is identical.
         *
         * @param node never {@code null}.
         * @return the pretty-printed JSON, never {@code null}.
         * @throws JsonProcessingException if Jackson cannot serialize the node.
         */
        private static String prettyPrint(JsonNode node) throws JsonProcessingException {
          return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(node);
        }

        /**
         * Shared Jackson {@link ObjectMapper}. T2.17 migration (issue #139): replaces the
         * json-lib {@code JSONSerializer.toJSON(Object)} POJO-conversion call with Jackson's
         * {@code ObjectMapper#valueToTree(Object)}.
         */
        private static final ObjectMapper MAPPER = new ObjectMapper();
      }
