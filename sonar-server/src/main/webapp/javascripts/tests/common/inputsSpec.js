(function() {
  var $;

  $ = jQuery;

  describe('WORK_DUR suite', function() {
    beforeEach(function() {
      window.SS = {};
      window.SS.phrases = {
        'work_duration': {
          'x_days': '{0}d',
          'x_hours': '{0}h',
          'x_minutes': '{0}min'
        }
      };
      this.input = $('<input type="text">');
      this.input.appendTo($('body'));
      return this.input.data('type', 'WORK_DUR');
    });
    it('converts', function() {
      this.input.originalVal('2d 7h 13min');
      return expect(this.input.val()).toBe(1393);
    });
    it('converts only days', function() {
      this.input.originalVal('1d');
      return expect(this.input.val()).toBe(480);
    });
    it('converts hours with minutes', function() {
      this.input.originalVal('2h 30min');
      return expect(this.input.val()).toBe(150);
    });
    it('restores', function() {
      this.input.val(1393);
      return expect(this.input.originalVal()).toBe('2d 7h 13min');
    });
    return it('returns initially incorrect value', function() {
      this.input.val('something');
      return expect(this.input.val()).toBe('something');
    });
  });

  describe('RATING suite', function() {
    beforeEach(function() {
      this.input = $('<input type="text">');
      this.input.appendTo($('body'));
      return this.input.data('type', 'RATING');
    });
    it('converts A', function() {
      this.input.originalVal('A');
      return expect(this.input.val()).toBe(1);
    });
    it('converts B', function() {
      this.input.originalVal('B');
      return expect(this.input.val()).toBe(2);
    });
    it('converts E', function() {
      this.input.originalVal('E');
      return expect(this.input.val()).toBe(5);
    });
    it('does not convert F', function() {
      this.input.originalVal('F');
      return expect(this.input.val()).toBe('F');
    });
    it('restores A', function() {
      this.input.val(1);
      return expect(this.input.originalVal()).toBe('A');
    });
    it('restores E', function() {
      this.input.val(5);
      return expect(this.input.originalVal()).toBe('E');
    });
    return it('returns initially incorrect value', function() {
      this.input.val('something');
      return expect(this.input.val()).toBe('something');
    });
  });

}).call(this);