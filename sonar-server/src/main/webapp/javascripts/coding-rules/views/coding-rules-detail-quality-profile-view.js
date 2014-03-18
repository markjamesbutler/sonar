(function() {
  var __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  define(['backbone.marionette', 'common/handlebars-extensions'], function(Marionette) {
    var CodingRulesDetailQualityProfilesView;
    return CodingRulesDetailQualityProfilesView = (function(_super) {
      __extends(CodingRulesDetailQualityProfilesView, _super);

      function CodingRulesDetailQualityProfilesView() {
        return CodingRulesDetailQualityProfilesView.__super__.constructor.apply(this, arguments);
      }

      CodingRulesDetailQualityProfilesView.prototype.className = 'coding-rules-detail-quality-profile';

      CodingRulesDetailQualityProfilesView.prototype.template = getTemplate('#coding-rules-detail-quality-profile-template');

      CodingRulesDetailQualityProfilesView.prototype.ui = {
        severitySelect: '.coding-rules-detail-quality-profile-severity',
        note: '.coding-rules-detail-quality-profile-note',
        noteForm: '.coding-rules-detail-quality-profile-note-form',
        noteText: '.coding-rules-detail-quality-profile-note-text',
        noteAdd: '.coding-rules-detail-quality-profile-note-add',
        noteEdit: '.coding-rules-detail-quality-profile-note-edit',
        noteDelete: '.coding-rules-detail-quality-profile-note-delete',
        noteCancel: '.coding-rules-detail-quality-profile-note-cancel',
        noteSubmit: '.coding-rules-detail-quality-profile-note-submit'
      };

      CodingRulesDetailQualityProfilesView.prototype.events = {
        'click @ui.noteAdd': 'editNote',
        'click @ui.noteEdit': 'editNote',
        'click @ui.noteDelete': 'deleteNote',
        'click @ui.noteCancel': 'cancelNote',
        'click @ui.noteSubmit': 'submitNote'
      };

      CodingRulesDetailQualityProfilesView.prototype.editNote = function() {
        this.ui.note.hide();
        this.ui.noteForm.show();
        return this.ui.noteText.focus();
      };

      CodingRulesDetailQualityProfilesView.prototype.deleteNote = function() {
        this.ui.noteText.val('');
        return this.submitNote().done((function(_this) {
          return function() {
            _this.model.unset('note');
            return _this.render();
          };
        })(this));
      };

      CodingRulesDetailQualityProfilesView.prototype.cancelNote = function() {
        this.ui.note.show();
        return this.ui.noteForm.hide();
      };

      CodingRulesDetailQualityProfilesView.prototype.submitNote = function() {
        this.ui.note.html('<i class="spinner"></i>');
        this.ui.noteForm.html('<i class="spinner"></i>');
        return jQuery.ajax({
          type: 'POST',
          url: "" + baseUrl + "/api/codingrules/note",
          dataType: 'json',
          data: {
            text: this.ui.noteText.val()
          }
        }).done((function(_this) {
          return function(r) {
            _this.model.set('note', r.note);
            return _this.render();
          };
        })(this));
      };

      CodingRulesDetailQualityProfilesView.prototype.onRender = function() {
        var format;
        this.ui.noteForm.hide();
        format = function(state) {
          if (!state.id) {
            return state.text;
          }
          return "<i class='icon-severity-" + (state.id.toLowerCase()) + "'></i> " + state.text;
        };
        this.ui.severitySelect.val(this.model.get('severity'));
        return this.ui.severitySelect.select2({
          width: '200px',
          minimumResultsForSearch: 999,
          formatResult: format,
          formatSelection: format,
          escapeMarkup: function(m) {
            return m;
          }
        });
      };

      CodingRulesDetailQualityProfilesView.prototype.getParent = function() {
        if (!this.model.get('inherits')) {
          return null;
        }
        return this.options.qualityProfiles.findWhere({
          key: this.model.get('inherits')
        }).toJSON();
      };

      CodingRulesDetailQualityProfilesView.prototype.enhanceParameters = function() {
        var parameters, parent;
        parent = this.getParent();
        parameters = this.model.get('parameters');
        if (!parent) {
          return parameters;
        }
        return parameters.map(function(p) {
          return _.extend(p, {
            original: _.findWhere(parent.parameters, {
              key: p.key
            }).value
          });
        });
      };

      CodingRulesDetailQualityProfilesView.prototype.serializeData = function() {
        return _.extend(CodingRulesDetailQualityProfilesView.__super__.serializeData.apply(this, arguments), {
          parent: this.getParent(),
          parameters: this.enhanceParameters(),
          severities: ['BLOCKER', 'CRITICAL', 'MAJOR', 'MINOR', 'INFO']
        });
      };

      return CodingRulesDetailQualityProfilesView;

    })(Marionette.ItemView);
  });

}).call(this);